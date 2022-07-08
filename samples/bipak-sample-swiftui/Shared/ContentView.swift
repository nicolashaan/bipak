//
// BiPaK library sample swift code
// Copyright 2022 Nicolas Haan.
//
import SwiftUI
import SampleCommon
import KMPNativeCoroutinesCore


class ItemListViewModel: ObservableObject {
    
    @Published var pagingData: PagingData<DomainItem> = empyPagingData()
    
    
    let dataSource: ItemDataSource
    let repository: ItemRepository
    let eventEmitter: PagingEventEmitter
    
    init() {
        // Instanciate the ItemRepository for our sample
        dataSource = ItemDataSource(totalCount: 100)
        repository = ItemRepository(dataSource: dataSource, pageSize: 10)
        
        // Create PagingEventEmitter needed to control the paging
        eventEmitter = PagingEventEmitter()
        
        
        // Will simulate an error on page 5
        GlobalServiceLocator.shared.errorOnPage = 5
        
        // Link the PagingEventEmitter to the pager hosted by the repository
        repository.setViewEventFlow(eventFlow: eventEmitter.eventFlow)
        
        // Request 1st item to trigger 1st page fetching
        eventEmitter.onGetItem(index: 0)
        
        let nativeFlow: NativeFlow = repository.getItemFlowNative()
        _ = nativeFlow({ pagingData, unit in
            
            // State should be updated on main thread
            DispatchQueue.main.async {
                self.pagingData = pagingData
            }
            
            return unit
        }, { error, unit in
            
            return unit
        })
        
    }
}

struct ContentView: View {
    
    @ObservedObject private var viewModel = ItemListViewModel()
    
    
    var body: some View {
        ScrollView {
            LazyVStack {
                ForEach(
                    Array(viewModel.pagingData.list.enumerated()),
                    id: \.offset
                ) { index, element in
                    PagingView(eventEmitter: viewModel.eventEmitter, id: index) {
                        Text(element.content)
                    }
                }
                if(viewModel.pagingData.state is PagingDataLoadState.Loading) {
                    HStack {
                        ProgressView()
                    }
                }
                if let state = viewModel.pagingData.state as? PagingDataLoadState.Error {
                    HStack {
                        Text(state.error.message ?? "Unknown error")
                        Button("Retry") {
                            viewModel.eventEmitter.retry()
                        }
                    }
                }
                
            }
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}

