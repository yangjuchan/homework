//
//  Puzzle.h
//  EightPuzzleConsole
//
//  Created by entropy on 09. 03. 28.
//  Copyright 2009 .... All rights reserved.
//

#import <Foundation/Foundation.h>


@interface Puzzle : NSObject {
	NSMutableArray *arrayState;
	int depth;
	int unmatched;
}

-(int)getUnmatched;

@property(readwrite, assign) NSMutableArray *arrayState;
@property(readwrite, assign) int depth;
@property(readwrite, assign) int unmatched;

@end
