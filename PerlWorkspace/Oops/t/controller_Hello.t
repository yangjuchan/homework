use strict;
use warnings;
use Test::More tests => 3;

BEGIN { use_ok 'Catalyst::Test', 'Oops' }
BEGIN { use_ok 'Oops::Controller::Hello' }

ok( request('/hello')->is_success, 'Request should succeed' );


