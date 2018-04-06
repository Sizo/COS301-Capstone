//
//  SignUpViewController.swift
//  Harvest
//
//  Created by Letanyan Arumugam on 2018/03/26.
//  Copyright © 2018 Letanyan Arumugam. All rights reserved.
//

import UIKit

class SignUpViewController: UIViewController {

  @IBOutlet weak var firstnameTextField: UITextField!
  @IBOutlet weak var lastnameTextField: UITextField!
  @IBOutlet weak var usernameTextField: UITextField!
  @IBOutlet weak var passwordTextField: UITextField!
  @IBOutlet weak var signUpButton: UIButton!
  @IBOutlet weak var cancelButton: UIButton!
  @IBOutlet weak var activityIndicator: UIActivityIndicatorView!
  
  
  @IBAction func signUpTouchUp(_ sender: UIButton) {
    guard let username = usernameTextField.text else {
      let alert = UIAlertController.alertController(
        title: "No email address provided",
        message: "Please provide an email address to create an account")
      
      present(alert, animated: true, completion: nil)
      
      return
    }
    
    let password = passwordTextField.text ?? ""
    
    let emailRegex = try! NSRegularExpression(
      pattern: "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}")

    let urange = NSMakeRange(0, username.count)
    let match = emailRegex.rangeOfFirstMatch(in: username, range: urange)

    guard match == urange else {
      let alert = UIAlertController.alertController(
        title: "Invalid Email Address",
        message: "Please provide a valid email address")

      present(alert, animated: true, completion: nil)

      return
    }
    
    guard let fname = firstnameTextField.text else {
      let alert = UIAlertController.alertController(
        title: "No first name provided",
        message: "Please provide a first name to create an account")
      
      present(alert, animated: true, completion: nil)
      
      return
    }
    
    guard let lname = lastnameTextField.text else {
      let alert = UIAlertController.alertController(
        title: "No last name provided",
        message: "Please provide a last name to create an account")
      
      present(alert, animated: true, completion: nil)
      
      return
    }
    
    signUpButton.isHidden = true
    cancelButton.isEnabled = false
    activityIndicator.startAnimating()
    HarvestDB.signUp(withEmail: username, andPassword: password, name: (fname, lname), on: self) {w in
      if w {
        self.dismiss(animated: true, completion: nil)
      }
      self.signUpButton.isHidden = false
      self.cancelButton.isEnabled = true
      self.activityIndicator.stopAnimating()
    }
  }
  
  @IBAction func cancelTouchUp(_ sender: Any) {
    dismiss(animated: true, completion: nil)
  }
  
  override func viewDidLoad() {
    super.viewDidLoad()
    hideKeyboardWhenTappedAround()
    
    signUpButton.apply(gradient: .green)
    cancelButton.apply(gradient: .blue)
  }

  override func didReceiveMemoryWarning() {
    super.didReceiveMemoryWarning()
  }

  override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
    
  }
}