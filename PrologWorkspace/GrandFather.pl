sexual("Bill", "male").
sexual("John", "male").
sexual("Pam", "male").
sexual("Lisa", "female").
sexual("Ada", "female").

father("Bill", "John").
father("Pam", "Bill").

mother("Pam", "Lisa").
mother("Bill", "Ada").

couple("Bill", "Lisa").
couple("John", "Ada").


parent(Person, Parent) :-
    father(Person, Parent);
    mother(Person, Parent).

children(Person, Children) :-
    father(Child, Person);
    mother(Child, Person).

grandFather(Person, GrandFather) :-
    sexual(GrandFather, male),
    father(Person, Father),
    father(Father, GrandFather).
grandFather(Person, GrandFather) :-
    sexual(GrandFather, male),
    mother(Person, Mother),
    father(Mother, GrandFather).

grandMother(Person, GrandMother) :-
    sexual(GrandMother, female),
    mother(Person, Mother),
    mother(Mother, GrandMother).
grandMother(Person, GrandMother) :-
    sexual(GrandMother, female),
    father(Person, Father),
    mother(Father, GrandMother).

ancestor(Person, Ancestor) :-
    parent(Person, Ancestor).
ancestor(Person, Ancestor) :-
    parent(Person, P1),
    parent(P1, Ancestor).
ancestor(Person, Ancestor) :-
    parent(Person, P1),
    parent(P1, P2),
    parent(P2, Ancestor).

descendant(Person, Descendant) :-
    children(Person, Descendant).
descendant(Person, Descendant) :-
    children(Person, D1),
    parent(D1, Descendant).
descendant(Person, Descendant) :-
    children(Person, D1),
    children(D1, D2),
    parent(D2, Descendant).
