package com.tiza.truck.api.support.facade;

import com.tiza.truck.api.support.facade.dto.CommandSend;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Description: CommandSendJpa
 * Author: DIYILIU
 * Update: 2019-07-01 14:05
 */
public interface CommandSendJpa extends JpaRepository<CommandSend, Long> {

}
