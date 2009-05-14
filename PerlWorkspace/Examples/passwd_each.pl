#!/opt/perl/bin/perl -w
%words = qw(
    fred    camel
    barney  llama
    betty   alpaca
    wilma   alpaca
);

print "What is your name? ";
$name = <STDIN>;
chomp($name);

if ($name =~ /^randal\b/i) {
    print "Hello, Randal! How good of you to be here!\n";
}
else {
    print "Hello, $name!\n";
    $secretword = $words{$name};

    if ($secretword eq "") {
	$secretword = "groucho";
    }

    print "What is the secret word? ";
    $guess = <STDIN>;
    chomp($guess);

    while ($guess ne $secretword) {
	print "Wrong, try again. What is the secret word? ";
	$guess = <STDIN>;
	chomp($guess);
    }
}
