#! /usr/bin/ruby
# vim: ts=2 sts=4 sw=2 
#
# EightPuzzle.rb : A Ruby 8-Puzzle program
# Author : Ju-chan Yang
# Required : Ruby 1.6 or higher
#

=begin
    1. Ruby에서의 모든 것은 어떤 종류의 객체(Object)이다.
    2. Duck type : 만일, 객체가 오리처럼 걷고 오리처럼 말한다면
	인터프리터는 아무 걱정 없이 그것을 오리로 취급한다.
    3. 기본적인 Ruby 배열의 push와 pop은 Stack처럼 작용한다.
    4. 
=end

END_STATE = "123456780"		# 퍼즐의 최종 목적 상태 저장값
$state = Array.new		# 퍼즐의 현재 상태 저장 배열 (Duck type)
$move_start = Array.new		# 퍼즐이 움직인 상태 저장 배열 (Duck type)
$prev_state = Array.new		# 퍼즐이 움직이기 전 상태 저장 배열 (Duck type)
$check_state = Hash.new		# 평가를 위한 퍼즐 상태 저장 해쉬

# 이동경로에 따른 값 저장 배열
$adjacent = [ ]
$adjacent[0] =  [1, 3]		# 0
$adjacent[1] =  [0, 4, 2]	# 1
$adjacent[2] =  [1, 5]		# 2
$adjacent[3] =  [0, 4, 6]	# 3
$adjacent[4] =  [1, 3, 5, 7]	# 4
$adjacent[5] =  [2, 4, 8]	# 5
$adjacent[6] =  [3, 7]		# 6
$adjacent[7] =  [4, 6, 8]	# 7
$adjacent[8] =  [5, 7]		# 8

# 퍼즐 상태 출력 메소드
def print_state(s)
    print "  ", "-" * 10,"\n"
    for c in [0,3,6]
	print " " * 3
	for r in 0..2
	    print " ", s[c+r].chr, " "
	end
	print "\n"
    end
    print "  ", "-" * 10, "\n\n"
=begin
    print s[0..2],"\n",s[3..5],"\n",s[6..8],"\n\n"
=end
end

# 퍼즐이 이동한 경로들 (History) 출력 메소드
def print_history(n)
    print "Number of Checked States is ",$check_state.size,"\n"
    counter = 0
    memo = [ ]
    
    while ( n != -1 )	# memo 배열에 현재까지 이동한 퍼즐 상태 push
	memo.push(n)
	n = $prev_state[n]
    end

    while (memo != [ ])	# 루프 돌면서 Depth에 따른 퍼즐 상태 출력 (pop)
	print "Step ",counter,"...\n"
	print_state($state[memo.pop])
	counter += 1 
    end

    print "Step ",counter,"...\n"
    print_state(END_STATE)
end

# 퍼즐 탐색 메소드
def search(s)
    move = 1;		# Depth
    count = 1;		# 이동 카운터
    $state[0] = s	# 첫번째 초기상태 저장 
    $move_start[0] = 0	# 
    $prev_state[0] = -1 # 이전 상태
    $check_state[s] = 1 # 이동 평가 상태 퍼즐
    while (count > $move_start[move - 1])
	i = $move_start[move- 1];
	STDERR.print "Search count (DEPTH) #{move} ...\n\n";
	$move_start[move] = count;
	while (i < $move_start[move])
	    zp = $state[i].index('0')	# GAP 의 위치

	    $adjacent[zp].each { |np|	# GAP 위치에 따른 이동 경로 평가
		c  = $state[i][np].chr  # 현재 상태의 GAP 이동 위치 대상값 저장
		ns = $state[i]          # Depth i의 퍼즐 상태 저장
		ns = ns.sub(/([0#{c}])(.*)([0#{c}])/,'\3\2\1')
		# Pattern maching : 패턴 매칭을 통한 퍼즐 배열 치환 (값 교환)
		# 모든 것은 object이기 때문에 sub 메서드를 이용하여
		# 퍼즐의 실질적인 GAP 이동
		if  (END_STATE == ns)   # 최종 상태와 Depth i의 배열이 같으면
		    puts "***** Solved this puzzle! *******"
		    print_history( i )
		return			# 퍼즐 탐색 해결
		elsif ($check_state[ns] != 1)
		    $state[count] = ns
		    $check_state[ns] = 1
		    count += 1
		    $prev_state[count] = i
		end
	    }
	i += 1
	end
    # 
    move +=1
    end
end


InitialPhase = "102345678"  # 탐색을 위한 퍼즐의 현재 상태 

print "initial state is ..\n"
print_state(InitialPhase)   # 초기 상태 출력

search(InitialPhase)	    # 탐색 시작
