#import <Foundation/Foundation.h>
#import "Puzzle.h"
#import "JAPriorityQueue.h"

int main (int argc, const char * argv[]) {
    NSAutoreleasePool * pool = [[NSAutoreleasePool alloc] init];
	
    // insert code here...
	
	JAPriorityQueue *priorityQueue = [[JAPriorityQueue alloc] initWithComparator:@selector(getUnmatched:)];
	NSEnumerator *enumerator = [priorityQueue objectEnumerator];
	id object;
	//[priorityQueue initWithComparator:@selector(unmatched)];
	
	Puzzle *temp1 = [[Puzzle alloc] init];
	Puzzle *temp2 = [[Puzzle alloc] init];
	Puzzle *temp3 = [[Puzzle alloc] init];
	Puzzle *temp4 = [[Puzzle alloc] init];
	Puzzle *temp5 = [[Puzzle alloc] init];
	
	[temp5 setUnmatched:1];
	[temp3 setUnmatched:10];
	[temp2 setUnmatched:33];
	[temp1 setUnmatched:41];
	[temp4 setUnmatched:55];
	
	[priorityQueue addObject:temp1];
	[priorityQueue addObject:temp2];
	[priorityQueue addObject:temp3];
	[priorityQueue addObject:temp4];
	[priorityQueue addObject:temp5];
	
	while ((object = [enumerator nextObject])) {
		NSLog(@"Value is %d", [object unmatched]);
	}
		
	[pool drain];
    return 0;
}
