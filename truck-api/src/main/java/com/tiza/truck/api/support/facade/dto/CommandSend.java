package com.tiza.truck.api.support.facade.dto;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

/**
 * Description: CommandSend
 * Author: DIYILIU
 * Update: 2019-07-01 14:05
 */

@Data
@Entity
@Table(name = "serv_command_send")
public class CommandSend {

    @Id
    private Long id;

    private String deviceId;

    private String terminalNo;

    @Column(name = "command_id")
    private Integer cmdId;

    private String sendData;

    private Date sendTime;

    private String responseData;

    private Date responseTime;

    private Integer errorCode;

    private Integer serialId;

    @Column(name = "send_status")
    private Integer status;

}
