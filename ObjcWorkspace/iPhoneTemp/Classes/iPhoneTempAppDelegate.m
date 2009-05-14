//
//  iPhoneTempAppDelegate.m
//  iPhoneTemp
//
//  Created by entropy on 09. 03. 12.
//  Copyright ... 2009. All rights reserved.
//

#import "iPhoneTempAppDelegate.h"
#import "iPhoneTempViewController.h"

@implementation iPhoneTempAppDelegate

@synthesize window;
@synthesize viewController;


- (void)applicationDidFinishLaunching:(UIApplication *)application {    
    
    // Override point for customization after app launch    
    [window addSubview:viewController.view];
    [window makeKeyAndVisible];
}


- (void)dealloc {
    [viewController release];
    [window release];
    [super dealloc];
}


@end
