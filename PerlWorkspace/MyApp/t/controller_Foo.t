use strict;
use warnings;
use Test::More tests => 3;

BEGIN { use_ok 'Catalyst::Test', 'MyApp' }
BEGIN { use_ok 'MyApp::Controller::Foo' }

ok( request('/foo')->is_success, 'Request should succeed' );


