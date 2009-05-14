#!/usr/bin/perl -w

use strict;
use DBI;

print "Here's a list of DBI drivers:\n";

my @available_drivers = DBI->available_drivers('quiet');
my $driver;

foreach $driver (@available_drivers) {
    print "$driver\n";
}
