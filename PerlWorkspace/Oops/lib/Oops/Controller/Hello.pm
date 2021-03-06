package Oops::Controller::Hello;

use strict;
use warnings;
use parent 'Catalyst::Controller';

=head1 NAME

Oops::Controller::Hello - Catalyst Controller

=head1 DESCRIPTION

Catalyst Controller.

=head1 METHODS

=cut


=head2 index

=cut

sub index :Path :Args(0) {
    my ( $self, $c ) = @_;

    $c->stash->{title} = 'My Catalyst First Example';
    $c->stash->{name1} = '주차닝';
    $c->stash->{name2} = '내굥이';
   
    $c->stash->{template} = 'hello.tt';
}


=head1 AUTHOR

entropy

=head1 LICENSE

This library is free software. You can redistribute it and/or modify
it under the same terms as Perl itself.

=cut

1;
