//
// BiPaK library sample swift code
// Copyright 2022 Nicolas Haan.
//
import UIKit

class ErrorTableViewCell: UITableViewCell {
    
    @IBOutlet var errorLabel: UILabel!
    
    var onRetry: ()->Void = {}
    
    static let identifier = "ErrorTableViewCell"
    
    static func nib() -> UINib {
        return UINib(nibName: "ErrorTableViewCell", bundle: nil)
    }
    
    public func configure(with errorMessage: String, onRetry: @escaping ()->Void){
        errorLabel.text = errorMessage
        self.onRetry = onRetry
    }
    
    @IBAction func onClick(_ sender: UIButton, forEvent event: UIEvent){
        onRetry()
    }
    
}
