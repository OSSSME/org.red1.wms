/*** Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,* and your worldly gain shall come to naught and those who share shall gain eventually above you.* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)*/package org.wms.model;

import java.sql.ResultSet;
import java.util.Properties;import org.adempiere.exceptions.AdempiereException;import org.compiere.model.Query;import org.compiere.util.Env;

public class MWM_DeliveryScheduleLine extends X_WM_DeliveryScheduleLine{

	private static final long serialVersionUID = -1L;

	public MWM_DeliveryScheduleLine(Properties ctx, int id, String trxName) {
		super(ctx,id,trxName);
		}

	public MWM_DeliveryScheduleLine(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}		protected boolean beforeSave (boolean newRecord)	{		if (getQtyDelivered().compareTo(getQtyOrdered())<0){			setIsBackOrder(true); 		}		else 			setIsBackOrder(false);				//check if has previous BackOrder that is not complete (no QtyDelivered value) so disallow any new BackOrders		if (getC_OrderLine().getQtyDelivered().compareTo(Env.ZERO)>0){			setQtyOrdered(getC_OrderLine().getQtyOrdered().subtract(getC_OrderLine().getQtyDelivered()));			setQtyDelivered(getC_OrderLine().getQtyOrdered().subtract(getC_OrderLine().getQtyDelivered()));		} else {			//check if has previous WM_InOut (backorder case) and if QtyDelivered then error of premature process			MWM_DeliveryScheduleLine prevDsLine = new Query(Env.getCtx(),MWM_DeliveryScheduleLine.Table_Name,MWM_DeliveryScheduleLine.COLUMNNAME_C_OrderLine_ID+"=?",get_TrxName())					.setParameters(getC_OrderLine_ID())					.first();			if (prevDsLine!=null && prevDsLine.isBackOrder())				throw new AdempiereException("Back Order premature. Complete Material Receipt first");			setQtyOrdered(getC_OrderLine().getQtyOrdered());			setQtyDelivered(getC_OrderLine().getQtyOrdered());		}		return true;	}
}
