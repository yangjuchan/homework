require "matrix"
require "pp"

class Hopfield
    attr_reader :output_pattern
    
    def initialize(pattern_size)
        @pattern_size = pattern_size
        @w = Matrix[ *[].fill([].fill(0, 0, @pattern_size), 0, @pattern_size) ]
    end
    
    def learn(pattern)
        delta_w = Matrix[pattern].t * Matrix[pattern]        
        @w += delta_w - Matrix.I(@pattern_size)
    end
    
    def recall(input_pattern, iteration_sequence)
        old_output = [].fill(rand, 0, @pattern_size)
        new_output = input_pattern
        
        while true
            iteration_sequence.each do |iter|
                net = input_pattern[iter] + Vector[*input_pattern].inner_product(@w.column(iter))
                new_output[iter] = af(net, new_output[iter])
                print "#{iter+1} : "
                pp new_output
            end
            puts
            if old_output == new_output
                @output_pattern = new_output
                break
            else
                old_output = new_output
            end
        end
    end
    
    private
    def af(x, net)
        if x > 0
            +1
        elsif x == 0
            net
        else
            -1
        end
    end
end

pattern1 = [ [ 1,  1,  1,  1,  1],
             [-1, -1, -1, -1,  1],
             [-1, -1, -1, -1,  1],
             [-1, -1, -1, -1,  1],
             [-1, -1, -1, -1,  1] ]
             
pattern2 = [ [ 1, -1, -1, -1, -1],
             [ 1, -1, -1, -1, -1],
             [ 1, -1, -1, -1, -1],
             [ 1, -1, -1, -1, -1],
             [ 1,  1,  1,  1,  1] ]
             
pattern3 = [ [ 1, -1, -1, -1, -1],
             [ 1, -1, -1, -1, -1],
             [ 1, -1, -1, -1, -1],
             [ 1, -1, -1, -1, -1],
             [ 1,  1,  1,  1,  1] ]

pattern4 = [ [ 1, -1, -1, -1, -1],
             [ 1, -1, -1, -1, -1],
             [ 1, -1, -1, -1, -1],
             [ 1, -1, -1, -1, -1],
             [ 1,  1,  1,  1,  1] ]

net = Hopfield.new(5)

pattern1.each do |i|
    net.learn i
end

pattern2.each do |i|
    net.learn i
end

pattern3.each do |i|
    net.learn i
end

pattern4.each do |i|
    net.learn i
end

net.recall [+1, -1, -1, -1], [1, 0, 3, 2]
puts "sequence [4, 1, 3, 2]"
net.recall [+1, -1, -1, -1], [3, 0, 2, 1]