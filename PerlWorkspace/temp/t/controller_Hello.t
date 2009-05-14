use strict;
use warnings;
use Test::More tests => 3;

BEGIN { use_ok 'Catalyst::Test', 'temp' }
BEGIN { use_ok 'temp::Controller::Hello' }

ok( request('/hello')->is_success, 'Request should succeed' );


