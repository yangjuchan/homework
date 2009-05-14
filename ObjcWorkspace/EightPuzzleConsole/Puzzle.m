//
//  Puzzle.m
//  EightPuzzleConsole
//
//  Created by entropy on 09. 03. 28.
//  Copyright 2009 .... All rights reserved.
//

#import "Puzzle.h"

@implementation Puzzle

@synthesize arrayState;
@synthesize depth;
@synthesize unmatched;

-(id)init
{
	[super init];
	
	depth = 0;
	unmatched = 9;
	arrayState = [NSArray arrayWithObjects:0,0,0,0,0,0,0,0,0,nil];
	
	return self;
}

-(int)getUnmatched
{
	return [self unmatched];
}

@end
