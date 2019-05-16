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

/** Generated Model for WM_TypeTable
 *  @author iDempiere (generated) 
 *  @version Release 6.2 - $Id$ */
public class X_WM_TypeTable extends PO implements I_WM_TypeTable, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20190515L;

    /** Standard Constructor */
    public X_WM_TypeTable (Properties ctx, int WM_TypeTable_ID, String trxName)
    {
      super (ctx, WM_TypeTable_ID, trxName);
      /** if (WM_TypeTable_ID == 0)
        {
			setWM_TypeTable_ID (0);
        } */
    }

    /** Load Constructor */
    public X_WM_TypeTable (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_WM_TypeTable[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_M_Warehouse getM_Warehouse() throws RuntimeException
    {
		return (org.compiere.model.I_M_Warehouse)MTable.get(getCtx(), org.compiere.model.I_M_Warehouse.Table_Name)
			.getPO(getM_Warehouse_ID(), get_TrxName());	}

	/** Set Warehouse.
		@param M_Warehouse_ID 
		Storage Warehouse and Service Point
	  */
	public void setM_Warehouse_ID (int M_Warehouse_ID)
	{
		if (M_Warehouse_ID < 1) 
			set_Value (COLUMNNAME_M_Warehouse_ID, null);
		else 
			set_Value (COLUMNNAME_M_Warehouse_ID, Integer.valueOf(M_Warehouse_ID));
	}

	/** Get Warehouse.
		@return Storage Warehouse and Service Point
	  */
	public int getM_Warehouse_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Warehouse_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Product Key.
		@param ProductValue 
		Key of the Product
	  */
	public void setProductValue (String ProductValue)
	{
		set_Value (COLUMNNAME_ProductValue, ProductValue);
	}

	/** Get Product Key.
		@return Key of the Product
	  */
	public String getProductValue () 
	{
		return (String)get_Value(COLUMNNAME_ProductValue);
	}

	/** Set TypeTable.
		@param WM_TypeTable_ID TypeTable	  */
	public void setWM_TypeTable_ID (int WM_TypeTable_ID)
	{
		if (WM_TypeTable_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_WM_TypeTable_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_WM_TypeTable_ID, Integer.valueOf(WM_TypeTable_ID));
	}

	/** Get TypeTable.
		@return TypeTable	  */
	public int getWM_TypeTable_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WM_TypeTable_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_WS_Type getWS_Type() throws RuntimeException
    {
		return (I_WS_Type)MTable.get(getCtx(), I_WS_Type.Table_Name)
			.getPO(getWS_Type_ID(), get_TrxName());	}

	/** Set WS_Type_ID.
		@param WS_Type_ID WS_Type_ID	  */
	public void setWS_Type_ID (int WS_Type_ID)
	{
		if (WS_Type_ID < 1) 
			set_Value (COLUMNNAME_WS_Type_ID, null);
		else 
			set_Value (COLUMNNAME_WS_Type_ID, Integer.valueOf(WS_Type_ID));
	}

	/** Get WS_Type_ID.
		@return WS_Type_ID	  */
	public int getWS_Type_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WS_Type_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Aisle (X).
		@param X 
		X dimension, e.g., Aisle
	  */
	public void setX (String X)
	{
		set_Value (COLUMNNAME_X, X);
	}

	/** Get Aisle (X).
		@return X dimension, e.g., Aisle
	  */
	public String getX () 
	{
		return (String)get_Value(COLUMNNAME_X);
	}

	/** Set Bin (Y).
		@param Y 
		Y dimension, e.g., Bin
	  */
	public void setY (String Y)
	{
		set_Value (COLUMNNAME_Y, Y);
	}

	/** Get Bin (Y).
		@return Y dimension, e.g., Bin
	  */
	public String getY () 
	{
		return (String)get_Value(COLUMNNAME_Y);
	}

	/** Set Level (Z).
		@param Z 
		Z dimension, e.g., Level
	  */
	public void setZ (String Z)
	{
		set_Value (COLUMNNAME_Z, Z);
	}

	/** Get Level (Z).
		@return Z dimension, e.g., Level
	  */
	public String getZ () 
	{
		return (String)get_Value(COLUMNNAME_Z);
	}
}