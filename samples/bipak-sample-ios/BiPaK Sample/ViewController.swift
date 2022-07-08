//
// BiPaK library sample swift code
// Copyright 2022 Nicolas Haan.
//
import UIKit
import SampleCommon
import KMPNativeCoroutinesCore

class ViewController: UIViewController, UITableViewDelegate
{
    let dataSource: ItemDataSource
    let repository:ItemRepository
    let eventEmitter: PagingEventEmitter
    
    private lazy var tableViewDataSource = configureDataSource()
    
    required init?(coder aDecoder: NSCoder) {
        // Instanciate the ItemRepository for our sample
        dataSource = ItemDataSource(totalCount: 100)
        repository = ItemRepository(dataSource: self.dataSource, pageSize: 10)
        
        // Create PagingEventEmitter needed to control the paging
        eventEmitter = PagingEventEmitter()
        
        // Will trigger an error on page 3
        GlobalServiceLocator.shared.errorOnPage = 3
        
        // Link the PagingEventEmitter to the pager hosted by the repository
        repository.setViewEventFlow(eventFlow: eventEmitter.eventFlow)
        
        // Request 1st item to trigger 1st page fetching
        eventEmitter.onGetItem(index: 0)
        
        super.init(nibName: nil, bundle: nil)
    }
    
    
    func subscribeToRepositoryFlow() {
        let nativeFlow: NativeFlow = repository.getItemFlowNative()
        _ = nativeFlow({ pagingData, unit in

            // Convert our domain items to UiModel
            var items = pagingData.list.map({ domainItem in
                UiModel.Data(domainItem)
            })
            
            // Append Loading or Error model to list if needed
            switch pagingData.state {
            case is PagingDataLoadState.Loading:
                items.append(UiModel.Loading)
                
            case let err as PagingDataLoadState.Error:
                items.append(UiModel.Error(err.error.message ?? "Unknown error"))
            default: ()
            }
            
            // Populate NSDiffableDataSourceSnapshot with our UiModel list
            var snapshot = NSDiffableDataSourceSnapshot<Int, UiModel>()
            snapshot.appendSections([0])
            snapshot.appendItems(items, toSection: nil)
            
            // Apply snapshot to tableViewDataSource
            self.tableViewDataSource.apply(snapshot, animatingDifferences: false)
            return unit
        }, { error, unit in
            
            return unit
        })
        
    }
    
    private let tableView: UITableView = {
        let table = UITableView()
        table.register(ItemTableViewCell.self, forCellReuseIdentifier: ItemTableViewCell.identifier)
        table.register(LoadingTableViewCell.nib(), forCellReuseIdentifier: LoadingTableViewCell.identifier)
        table.register(ErrorTableViewCell.nib(), forCellReuseIdentifier: ErrorTableViewCell.identifier)
        return table
    }()
    
    
    override func viewDidLoad() {
        super.viewDidLoad()
        view?.addSubview(tableView)
        tableView.delegate = self
        tableView.dataSource = tableViewDataSource
        
        subscribeToRepositoryFlow()
        
        let notificationCenter = NotificationCenter.default
        notificationCenter.addObserver(
            self,
            selector: #selector(appMovedToBackground),
            name: UIApplication.didEnterBackgroundNotification,
            object: nil
        )
        
        notificationCenter.addObserver(
            self,
            selector: #selector(appMovedToForeground),
            name: UIApplication.didBecomeActiveNotification,
            object: nil
        )
    }
    
    @objc func appMovedToBackground() {
        eventEmitter.stop()
    }
    
    @objc func appMovedToForeground() {
        repository.setViewEventFlow(eventFlow: eventEmitter.eventFlow)
    }
    
    
    override func viewDidLayoutSubviews(){
        super.viewDidLayoutSubviews()
        tableView.frame = view.bounds
    }
    
    func configureDataSource() -> UiModelUITableViewDiffableDataSource {
        return UiModelUITableViewDiffableDataSource(
            tableView: tableView,
            cellProvider: { tableView, indexPath, data in
                
                switch data {
                case .Data(let item):
                    let cell = tableView.dequeueReusableCell(
                        withIdentifier: ItemTableViewCell.identifier,
                        for: indexPath)
                    // Calling event emitter with current index
                    self.eventEmitter.onGetItem(index: Int32(indexPath.row))
                    cell.textLabel?.text = item.content
                    return cell
                case .Loading:
                    let cell = tableView.dequeueReusableCell(
                        withIdentifier: LoadingTableViewCell.identifier,
                        for: indexPath)
                    return cell
                    
                case .Error(let error):
                    let cell = tableView.dequeueReusableCell(
                        withIdentifier: ErrorTableViewCell.identifier,
                        for: indexPath) as! ErrorTableViewCell
                    
                    cell.errorLabel.text = error
                    cell.onRetry = { self.eventEmitter.retry() }
                    
                    return cell
                }
            }
        )
    }
}

enum UiModel: Hashable {
    case Data(DomainItem)
    case Loading
    case Error(String)
}
