/**

import java.sql.ResultSet;
import java.util.Properties;

public class MWM_InOutLine extends X_WM_InOutLine{

	private static final long serialVersionUID = -1L;

	public MWM_InOutLine(Properties ctx, int id, String trxName) {
		super(ctx,id,trxName);
		}

	public MWM_InOutLine(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
}