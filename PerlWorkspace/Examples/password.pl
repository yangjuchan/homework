#!/opt/perl/bin/perl -w
$secretword = "llama";

print "What is your name? ";
$name = <STDIN>;
chomp $name;

if ($name eq "Randal") {
    print "Hello, Randal! How good of you to be here!\n";
} else {
    print "Hello, $name!\n";
    print "What is the secret word? ";
    
    $guess = <STDIN>;
    chomp($guess);

    while ($guess ne $secretword) {
	print "Wrong, try again. What is the secret word? ";
	$guess = <STDIN>;
	chomp($guess);
    }
}
