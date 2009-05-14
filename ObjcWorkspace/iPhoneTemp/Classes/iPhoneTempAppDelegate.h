//
//  iPhoneTempAppDelegate.h
//  iPhoneTemp
//
//  Created by entropy on 09. 03. 12.
//  Copyright ... 2009. All rights reserved.
//

#import <UIKit/UIKit.h>

@class iPhoneTempViewController;

@interface iPhoneTempAppDelegate : NSObject <UIApplicationDelegate> {
    UIWindow *window;
    iPhoneTempViewController *viewController;
}

@property (nonatomic, retain) IBOutlet UIWindow *window;
@property (nonatomic, retain) IBOutlet iPhoneTempViewController *viewController;

@end

