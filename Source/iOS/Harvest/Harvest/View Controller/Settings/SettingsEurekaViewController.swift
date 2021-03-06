//
//  SettingsEurekaViewController.swift
//  Harvest
//
//  Created by Letanyan Arumugam on 2018/04/21.
//  Copyright © 2018 University of Pretoria. All rights reserved.
//

import Eureka
import SCLAlertView

struct OrganizationInfo: CustomStringConvertible, Equatable {
  var uid: String?
  var name: String
  
  init(_ uid: String?, _ name: String) {
    self.uid = uid
    self.name = name
  }
  
  var description: String {
    return name
  }
  
  static func == (lhs: OrganizationInfo, rhs: OrganizationInfo) -> Bool {
    return lhs.uid == rhs.uid
  }
}

class SettingsEurekaViewController: ReloadableFormViewController {
  override func viewDidLoad() {
    super.viewDidLoad()
  }
  
  override func setUp() {
    let welcomeScreenRow = ButtonRow { row in
      row.title = "Welcome Screen"
    }.onCellSelection { _, _ in
      let vc = self.storyboard?.instantiateViewController(withIdentifier: "carouselViewController")
      let avc = vc as! CarouselViewController
      avc.showIntro()
      
      self.present(avc, animated: true, completion: nil)
    }
    
    let tutorialScreenRow = ButtonRow { row in
      row.title = "Tutorial"
    }.onCellSelection { _, _ in
      let vc = self.storyboard?.instantiateViewController(withIdentifier: "carouselViewController")
      let avc = vc as! CarouselViewController
      avc.showTutorial()
      
      self.present(avc, animated: true, completion: nil)
    }
    
    let userManualRow = ButtonRow { row in
      row.title = "User Manual"
    }.onCellSelection { _, _ in
      let vc = self.storyboard?.instantiateViewController(withIdentifier: "pdfViewController")
      let pvc = vc as! PDFViewController
      pvc.loadPDF(named: "HarvestUserManual")
      
//      self.navigationController?.present(pvc, animated: true, completion: nil)
      self.navigationController?.pushViewController(pvc, animated: true)
//      self.present(pvc, animated: true, completion: nil)
    }
    
    userSection(form: form)
    
    if HarvestUser.current.workingForID.isEmpty { // is farmer
      form
        +++ Section("Help")
        <<< tutorialScreenRow
        <<< userManualRow
        <<< welcomeScreenRow
      
    } else { // is foreman
      form
        +++ Section("Help")
        <<< tutorialScreenRow
    }
  }
  
  func userSection(form: Form) {
    let userRow = HarvestUser.current.accountIdentifier
    
    let adminRow = AdminRow(tag: nil, admin: HarvestUser.current) { row in
      row.title = "Admin"
    }
    
    let logoutRow = ButtonRow { row in
      row.title = "Logout"
    }.onCellSelection { (_, _) in
      HarvestDB.signOut { w in
        if w,
          let vc = self
            .storyboard?
            .instantiateViewController(withIdentifier: "signInOptionViewController") {
          self.present(vc, animated: true, completion: nil)
        }
      }
    }
    
    let resignRow = ButtonRow { row in
      row.title = "Resign"
    }.onCellSelection { (_, _) in
      let confirmationAlert = SCLAlertView(appearance: .warningAppearance)
      confirmationAlert.addButton("Cancel", action: {})
      confirmationAlert.addButton("Resign") {
        HarvestDB.resign { _, _ in
          if let vc = self.storyboard?.instantiateViewController(withIdentifier: "signInOptionViewController") {
            self.present(vc, animated: true, completion: nil)
          }
        }
      }
      
      confirmationAlert.showWarning(
        "Are You Sure?",
        subTitle: """
        Are you sure you want to resign from working for "\(HarvestUser.current.organisationName)"?
        """)
      
    }.cellUpdate { (cell, _) in
      cell.textLabel?.textColor = .white
      cell.backgroundColor = .red
    }
    
    if HarvestUser.current.workingForID.isEmpty { // is farmer
      form
        +++ Section(userRow)
        <<< adminRow
        <<< logoutRow
      
    } else { // is foreman
      form
        +++ Section(userRow)
        <<< logoutRow
        +++ Section()
        <<< resignRow
    }
  }
  
  override func tearDown() {
    form.removeAll()
  }
  
}
