package com.tiza.truck.rp.support.task;

import com.tiza.plugin.bean.VehicleInfo;
import com.tiza.plugin.cache.ICache;
import com.tiza.plugin.model.facade.ITask;
import com.tiza.plugin.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Description: VehicleInfoTask
 * Author: DIYILIU
 * Update: 2018-01-30 11:07
 */

@Slf4j
@Service
public class VehicleInfoTask implements ITask {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private ICache vehicleInfoProvider;

    @Scheduled(fixedRate = 5 * 60 * 1000, initialDelay = 1 * 1000)
    public void execute() {
        log.info("刷新设备列表 ...");

        String sql = "SELECT   " +
                "  `d`.`id` vehId,  " +
                "  `t`.`terminal_no` terminal,  " +
                "  `t`.`protocol_type` protocol," +
                "  `d`.`hardware_type` vehType, " +
                "  `su`.`id` unitId, " +
                "  `su`.`unit_name` unitName " +
                "FROM  " +
                "  `biz_terminal` t   " +
                "  INNER JOIN `biz_device` d   " +
                "    ON `d`.`terminal_no` = `t`.`terminal_no`   " +
                "    AND `d`.`is_deleted` = 0   " +
                "  LEFT JOIN `sys_unit` su " +
                "    ON `su`.`id` = `d`.`unit_id` " +
                "WHERE `t`.`is_deleted` = 0 ";

        List<VehicleInfo> vehicleInfos = jdbcTemplate.query(sql, new RowMapper<VehicleInfo>() {
            @Override
            public VehicleInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
                VehicleInfo vehicleInfo = new VehicleInfo();
                vehicleInfo.setId(rs.getLong("vehId"));
                vehicleInfo.setTerminalId(rs.getString("terminal"));
                vehicleInfo.setVehType(rs.getInt("vehType"));
                vehicleInfo.setProtocol(rs.getString("protocol"));
                vehicleInfo.setOwner(rs.getString("unitId"));
                vehicleInfo.setOwnerName(rs.getString("unitName"));

                return vehicleInfo;
            }
        });

        refresh(vehicleInfos, vehicleInfoProvider);
    }

    private void refresh(List<VehicleInfo> vehicleInfos, ICache vehicleCache) {
        if (vehicleInfos == null || vehicleInfos.size() < 1) {
            log.warn("无车辆信息！");
            return;
        }

        Set oldKeys = vehicleCache.getKeys();
        Set tempKeys = new HashSet(vehicleInfos.size());

        for (VehicleInfo vehicle : vehicleInfos) {
            vehicleCache.put(vehicle.getTerminalId(), vehicle);
            tempKeys.add(vehicle.getTerminalId());
        }
        CommonUtil.refreshCache(oldKeys, tempKeys, vehicleCache);
    }
}
