//
// BiPaK library sample swift code
// Copyright 2022 Nicolas Haan.
//

import UIKit

class LoadingTableViewCell: UITableViewCell {
    
    @IBOutlet var activityIndicator: UIActivityIndicatorView!
    
    static let identifier = "LoadingTableViewCell"
    
    static func nib() -> UINib {
        return UINib(nibName: "LoadingTableViewCell", bundle: nil)
    }
    
    override func prepareForReuse() {
           activityIndicator.startAnimating()
    }

}
