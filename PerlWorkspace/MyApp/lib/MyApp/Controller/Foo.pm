package MyApp::Controller::Foo;

use strict;
use warnings;
use parent 'Catalyst::Controller';

=head1 NAME

MyApp::Controller::Foo - Catalyst Controller

=head1 DESCRIPTION

Catalyst Controller.

=head1 METHODS

=cut


=head2 index

=cut

sub index :Path :Args(0) {
    my ( $self, $c ) = @_;

    $c->response->body('Matched MyApp::Controller::Foo in Foo.');
}


=head1 AUTHOR

entropy

=head1 LICENSE

This library is free software. You can redistribute it and/or modify
it under the same terms as Perl itself.

=cut

1;
