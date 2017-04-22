/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2012 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
/** Generated Model - DO NOT CHANGE */
package org.my.model;

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for WM_EmptyStorageLine
 *  @author iDempiere (generated) 
 *  @version Release 4.1 - $Id$ */
public class X_WM_EmptyStorageLine extends PO implements I_WM_EmptyStorageLine, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20170422L;

    /** Standard Constructor */
    public X_WM_EmptyStorageLine (Properties ctx, int WM_EmptyStorageLine_ID, String trxName)
    {
      super (ctx, WM_EmptyStorageLine_ID, trxName);
      /** if (WM_EmptyStorageLine_ID == 0)
        {
			setWM_EmptyStorageLine_ID (0);
        } */
    }

    /** Load Constructor */
    public X_WM_EmptyStorageLine (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 3 - Client - Org 
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuffer sb = new StringBuffer ("X_WM_EmptyStorageLine[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_C_UOM getC_UOM() throws RuntimeException
    {
		return (org.compiere.model.I_C_UOM)MTable.get(getCtx(), org.compiere.model.I_C_UOM.Table_Name)
			.getPO(getC_UOM_ID(), get_TrxName());	}

	/** Set UOM.
		@param C_UOM_ID 
		Unit of Measure
	  */
	public void setC_UOM_ID (int C_UOM_ID)
	{
		if (C_UOM_ID < 1) 
			set_Value (COLUMNNAME_C_UOM_ID, null);
		else 
			set_Value (COLUMNNAME_C_UOM_ID, Integer.valueOf(C_UOM_ID));
	}

	/** Get UOM.
		@return Unit of Measure
	  */
	public int getC_UOM_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_UOM_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_M_Product getM_Product() throws RuntimeException
    {
		return (org.compiere.model.I_M_Product)MTable.get(getCtx(), org.compiere.model.I_M_Product.Table_Name)
			.getPO(getM_Product_ID(), get_TrxName());	}

	/** Set Product.
		@param M_Product_ID 
		Product, Service, Item
	  */
	public void setM_Product_ID (int M_Product_ID)
	{
		if (M_Product_ID < 1) 
			set_Value (COLUMNNAME_M_Product_ID, null);
		else 
			set_Value (COLUMNNAME_M_Product_ID, Integer.valueOf(M_Product_ID));
	}

	/** Get Product.
		@return Product, Service, Item
	  */
	public int getM_Product_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Product_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set QtyMovement.
		@param QtyMovement QtyMovement	  */
	public void setQtyMovement (String QtyMovement)
	{
		set_Value (COLUMNNAME_QtyMovement, QtyMovement);
	}

	/** Get QtyMovement.
		@return QtyMovement	  */
	public String getQtyMovement () 
	{
		return (String)get_Value(COLUMNNAME_QtyMovement);
	}

	/** Set EmptyStorageLine.
		@param WM_EmptyStorageLine_ID EmptyStorageLine	  */
	public void setWM_EmptyStorageLine_ID (int WM_EmptyStorageLine_ID)
	{
		if (WM_EmptyStorageLine_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_WM_EmptyStorageLine_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_WM_EmptyStorageLine_ID, Integer.valueOf(WM_EmptyStorageLine_ID));
	}

	/** Get EmptyStorageLine.
		@return EmptyStorageLine	  */
	public int getWM_EmptyStorageLine_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WM_EmptyStorageLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_WM_EmptyStorage getWM_EmptyStorage() throws RuntimeException
    {
		return (I_WM_EmptyStorage)MTable.get(getCtx(), I_WM_EmptyStorage.Table_Name)
			.getPO(getWM_EmptyStorage_ID(), get_TrxName());	}

	/** Set EmptyStorage.
		@param WM_EmptyStorage_ID EmptyStorage	  */
	public void setWM_EmptyStorage_ID (int WM_EmptyStorage_ID)
	{
		if (WM_EmptyStorage_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_WM_EmptyStorage_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_WM_EmptyStorage_ID, Integer.valueOf(WM_EmptyStorage_ID));
	}

	/** Get EmptyStorage.
		@return EmptyStorage	  */
	public int getWM_EmptyStorage_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WM_EmptyStorage_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_WM_InOutLine getWM_InOutLine() throws RuntimeException
    {
		return (I_WM_InOutLine)MTable.get(getCtx(), I_WM_InOutLine.Table_Name)
			.getPO(getWM_InOutLine_ID(), get_TrxName());	}

	/** Set WM_InOutLine_ID.
		@param WM_InOutLine_ID WM_InOutLine_ID	  */
	public void setWM_InOutLine_ID (int WM_InOutLine_ID)
	{
		if (WM_InOutLine_ID < 1) 
			set_Value (COLUMNNAME_WM_InOutLine_ID, null);
		else 
			set_Value (COLUMNNAME_WM_InOutLine_ID, Integer.valueOf(WM_InOutLine_ID));
	}

	/** Get WM_InOutLine_ID.
		@return WM_InOutLine_ID	  */
	public int getWM_InOutLine_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WM_InOutLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}