package com.kpmg.rcm.sourcing.common.config;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class RCTableNameConfig extends SpringPhysicalNamingStrategy {
	@Value("${table.name.suffix:us}")
	private String tableNameSuffix;

	@Override
	public Identifier toPhysicalTableName(final Identifier identifier, final JdbcEnvironment jdbcEnv) {
		switch (identifier.getText()) {
		case "RecordChangeModel":
			log.debug("Record change table [{}] is selected", tableNameSuffix);
			return Identifier.toIdentifier("record_change_"+tableNameSuffix);
		case "ProcurementDetails":
			log.debug("ProcurementDetails table [{}] is selected", tableNameSuffix);
			return Identifier.toIdentifier("procurement_details_"+tableNameSuffix);
		default:
			return super.toPhysicalTableName(identifier, jdbcEnv);
		}
	}
}
