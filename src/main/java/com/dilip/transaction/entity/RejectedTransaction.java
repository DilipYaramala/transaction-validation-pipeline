package com.dilip.transaction.entity;
import jakarta.persistence.*;
@Entity @Table(name="rejected_transaction")
public class RejectedTransaction {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="run_id",nullable=false) private String runId;
 @Column(name="source_file",nullable=false) private String sourceFile;
 @Column(name="source_row",nullable=false) private int sourceRow;
 @Column(name="event_id") private String eventId;
 @Lob @Column(name="raw_record") private String rawRecord;
 @Column(nullable=false,length=500) private String reason;
 protected RejectedTransaction(){}
 public RejectedTransaction(String r,String f,int n,String e,String raw,String why){runId=r;sourceFile=f;sourceRow=n;eventId=e;rawRecord=raw;reason=why;}
 public String getReason(){return reason;} public String getEventId(){return eventId;} public String getRawRecord(){return rawRecord;}
}