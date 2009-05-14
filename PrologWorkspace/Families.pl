sexual(bill, male).
sexual(john, male).
sexual(pam, male).
sexual(tom, male).
sexual(michael, male).
sexual(lisa, female).
sexual(ada, female).
sexual(emma, female).
sexual(brittney, female).
sexual(emily, female).

father(bill, john).
father(tom, john).
father(pam, bill).
father(emily, bill).
father(lisa, michael).
father(emma, michael).

mother(pam, lisa).
mother(emily, lisa).
mother(bill, ada).
mother(tom, ada).
mother(lisa, brittney).
mother(emma, brittney).

%isEqual(P1, P2) :-
%    P1 = P2.

parent(Person, Parent) :-
    father(Person, Parent);
    mother(Person, Parent).

child(Person, Child) :-
    father(Child, Person);
    mother(Child, Person).
    
couple(Person, Couple) :-
    child(Person, Child1),
    child(Couple, Child2),
    Child1 = Child2,
    Person \= Couple.
    
sibling(P1, P2) :-
    father(P1, F1),
    mother(P1, M1),
    father(P2, F2),
    mother(P2, M2),
    F1 = F2,
    M1 = M2,
    P1 \= P2.

%grandFather(Person, GrandFather) :-
%    sexual(GrandFather, male),
%    father(Person, Father),
%    father(Father, GrandFather).
%grandFather(Person, GrandFather) :-
%    sexual(GrandFather, male),
%    mother(Person, Mother),
%    father(Mother, GrandFather).

%grandMother(Person, GrandMother) :-
%    sexual(GrandMother, female),
%    mother(Person, Mother),
%    mother(Mother, GrandMother).
%grandMother(Person, GrandMother) :-
%    sexual(GrandMother, female),
%    father(Person, Father),
%    mother(Father, GrandMother).

ancestor(Person, Ancestor) :-
    parent(Person, Ancestor),
    Person \= Ancestor.
ancestor(Person, Ancestor) :-
    parent(Person, P1),
    parent(P1, Ancestor),
    Person \= Ancestor,
    not(couple(Person, Ancestor)).
ancestor(Person, Ancestor) :-
    parent(Person, P1),
    parent(P1, P2),
    parent(P2, Ancestor),
    Person \= Ancestor,
    not(couple(Person, Ancestor)).

descendant(Person, Descendant) :-
    child(Person, Descendant).
descendant(Person, Descendant) :-
    child(Person, D1),
    child(D1, Descendant),
    Person \= Descendant,
    not(couple(Person, Descendant)).
descendant(Person, Descendant) :-
    child(Person, D1),
    child(D1, D2),
    child(D2, Descendant),
    Person \= Descendant,
    not(couple(Person, Descendant)).

uncle(Person, Uncle) :-
    sexual(Uncle, male),
    father(Person, Father),
    sibling(Father, Uncle),
    Father \= Uncle.
    
aunt(Person, Aunt) :-
    sexual(Aunt, female),
    mother(Person, Mother),
    sibling(Mother, Aunt),
    Mother \= Aunt.