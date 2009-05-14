//
//  PuzzleLists.h
//  EightPuzzleConsole
//
//  Created by entropy on 09. 03. 28.
//  Copyright 2009 .... All rights reserved.
//

#import <Foundation/Foundation.h>
#import "JAPriorityQueue.h"
#import "Puzzle.h"

@interface PuzzleLists : NSObject {
	JAPriorityQueue *priorityQueue;
	NSMutableArray *visitList;
	Puzzle *goalPuzzle;
	Puzzle *linkedStartPuzzle;
	int searchCount;
}

-(BOOL)searchGoalPuzzle;
-(void)expandNextStatePuzzle:(Puzzle *)puzzle;
-(Puzzle *)getLinkedStartPuzzle:(Puzzle *)puzzle;
-(void)printSearchPath;

@end
