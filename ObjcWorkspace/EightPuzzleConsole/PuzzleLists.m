//
//  PuzzleLists.m
//  EightPuzzleConsole
//
//  Created by entropy on 09. 03. 28.
//  Copyright 2009 .... All rights reserved.
//

#import "PuzzleLists.h"

@implementation PuzzleLists

-(void)init
{
	[self init];
	
	[priorityQueue initWithComparator:@selector(unmatched)];
	visitList = NULL;
	goalPuzzle = NULL;
	linkedStartPuzzle = NULL;
	searchCount = 0;
	
	return self;
}

-(BOOL)searchGoalPuzzle
{
	return TRUE;
}

-(void)expandNextStatePuzzle:(Puzzle *)puzzle
{
	
}

-(Puzzle *)getLinkedStartPuzzle:(Puzzle *)puzzle
{
	
	return linkedStartPuzzle;
}

-(void)printSearchPath
{
	
}

@end
