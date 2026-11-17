package io.github.zforgo.firqua.hibernate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
class Name {

    @Column(name = "FIRST_NAME")
    String firstName;

    @Column(name = "LAST_NAME")
    String lastName;
}
