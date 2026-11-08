package com.dilip.transaction.entity;
import jakarta.persistence.*; import java.math.BigDecimal; import java.time.Instant;
@Entity @Table(name="curated_transaction",uniqueConstraints=@UniqueConstraint(columnNames="event_id"))
public class CuratedTransaction {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="event_id",nullable=false,unique=true) private String eventId;
 @Column(name="transfer_id",nullable=false) private String transferId;
 @Column(name="leg_type",nullable=false) private String legType;
 @Column(name="account_id",nullable=false) private String accountId;
 @Column(nullable=false,precision=19,scale=4) private BigDecimal amount;
 @Column(nullable=false,length=3) private String currency;
 @Column(name="event_time",nullable=false) private Instant eventTime;
 @Column(name="source_file",nullable=false) private String sourceFile;
 @Column(name="run_id",nullable=false) private String runId;
 protected CuratedTransaction(){}
 public CuratedTransaction(String e,String t,String l,String a,BigDecimal m,String c,Instant time,String f,String r){
 eventId=e;transferId=t;legType=l;accountId=a;amount=m;currency=c;eventTime=time;sourceFile=f;runId=r;}
 public String getEventId(){return eventId;} public String getTransferId(){return transferId;}
 public String getLegType(){return legType;} public String getAccountId(){return accountId;}
 public BigDecimal getAmount(){return amount;} public String getCurrency(){return currency;}
 public Instant getEventTime(){return eventTime;} public String getSourceFile(){return sourceFile;} public String getRunId(){return runId;}
}