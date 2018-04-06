//
//  QuickAlert.swift
//  Harvest
//
//  Created by Letanyan Arumugam on 2018/03/26.
//  Copyright © 2018 Letanyan Arumugam. All rights reserved.
//

import UIKit

extension UIAlertController {
  static func alertController(title: String, message: String) -> UIAlertController {
    let alert = UIAlertController(
      title: title,
      message: message,
      preferredStyle: .alert)
    
    let okay = UIAlertAction(title: "Okay", style: .default, handler: nil)
    
    alert.addAction(okay)
    
    return alert
  }
}