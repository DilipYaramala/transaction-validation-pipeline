package com.dilip.transaction.entity;
import jakarta.persistence.*;
@Entity @Table(name="account")
public class Account {
 @Id @Column(name="account_id",length=40) private String accountId;
 protected Account(){}
 public Account(String id) {
	    this.accountId = id;
	}

	public String getAccountId() {
	    return accountId;
	}
}