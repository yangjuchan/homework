#!/opt/perl/bin/perl -w
print "What is your name? ";
$name = <STDIN>;
chomp($name);

if ($name eq "Randal") {
    print "Hello, Randal! How good of you to be here!\n";
} else {
    print "Hello, $name!\n";
}
