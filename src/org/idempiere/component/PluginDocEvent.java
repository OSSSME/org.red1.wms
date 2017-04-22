/******************************************************************************
 * Product: iDempiere Free ERP Project based on Compiere (2006)               *
 * Copyright (C) 2014 Redhuan D. Oon All Rights Reserved.                     *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *  FOR NON-COMMERCIAL DEVELOPER USE ONLY                                     *
 *  @author Redhuan D. Oon  - red1@red1.org  www.red1.org                     *
 *****************************************************************************/

package org.idempiere.component;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventTopics;
import org.adempiere.base.event.LoginEventData;
import org.compiere.model.MAsset;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.osgi.service.event.Event;

/**
 *  @author red1
 */
public class PluginDocEvent extends AbstractEventHandler{
	private static CLogger log = CLogger.getCLogger(PluginDocEvent.class); 
	private String trxName = "";
	private PO po = null;
	@Override
	protected void initialize() { 
	//register EventTopics and TableNames
		registerEvent(IEventTopics.DOC_AFTER_PREPARE, MAsset.Table_Name);
		log.info("<BLANK DOC EVENT> .. IS NOW INITIALIZED");
		}

	@Override
	protected void doHandleEvent(Event event) {
		setPo(getPO(event));
		setTrxName(po.get_TrxName());
		String type = event.getTopic();
		//testing that it works at login
		if (type.equals(IEventTopics.DOC_AFTER_PREPARE)) 
			if (po instanceof MAsset){
				 //DO SOMETHING
			}
	}  
	
	private void setPo(PO eventPO) {
		 po = eventPO;
	}

	private void setTrxName(String get_TrxName) {
	trxName = get_TrxName;
		}
}
