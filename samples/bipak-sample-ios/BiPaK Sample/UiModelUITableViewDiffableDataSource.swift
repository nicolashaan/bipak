//
// BiPaK library sample swift code
// Copyright 2022 Nicolas Haan.
//

import UIKit
import SampleCommon

class  UiModelUITableViewDiffableDataSource:
        UITableViewDiffableDataSource<Int, UiModel> {
    
    override func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        return nil
    }
}
