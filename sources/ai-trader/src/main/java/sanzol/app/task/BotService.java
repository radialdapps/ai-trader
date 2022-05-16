package sanzol.app.task;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.binance.client.RequestOptions;
import com.binance.client.SyncRequestClient;
import com.binance.client.model.trade.Order;
import com.binance.client.model.trade.PositionRisk;

import sanzol.app.config.PrivateConfig;
import sanzol.app.listener.BotListener;
import sanzol.app.service.SimpleTrader;
import sanzol.app.service.Symbol;
import sanzol.lib.util.BeepUtils;

public final class BotService
{
	private static boolean isTpRearrangement = false;
	private static Double tpPercent = null;

	private static boolean isSlRearrangement = false;
	private static Double slUsd = null;

	public static boolean isTpRearrangement()
	{
		return isTpRearrangement;
	}

	public static Double getTpPercent()
	{
		return tpPercent;
	}

	public static void setTpPercent(Double tpPercent)
	{
		BotService.tpPercent = tpPercent;
	}

	public static boolean isSlRearrangement()
	{
		return isSlRearrangement;
	}

	public static Double getSlUsd()
	{
		return slUsd;
	}

	public static void setSlUsd(Double slUsd)
	{
		BotService.slUsd = slUsd;
	}

	public static void setTpRearrangement(boolean isTpRearrangement)
	{
		info("TP Rearrangement " + String.valueOf(isTpRearrangement));
		BotService.isTpRearrangement = isTpRearrangement;
	}

	public static void setSlRearrangement(boolean isSlRearrangement)
	{
		info("SL Rearrangement " + String.valueOf(isSlRearrangement));
		BotService.isSlRearrangement = isSlRearrangement;
	}

	// ------------------------------------------------------------------------
	// TP & SL
	// ------------------------------------------------------------------------

	public synchronized static void onPositionUpdate()
	{
		List<PositionRisk> lstPositionRisk = PositionService.getLstPositionRisk();

		if (lstPositionRisk != null && !lstPositionRisk.isEmpty())
		{
			for (PositionRisk entry : lstPositionRisk)
			{
				if (entry.getPositionAmt().compareTo(BigDecimal.ZERO) != 0)
				{
					Symbol symbol = Symbol.getInstance(entry.getSymbol());
					if (symbol != null)
					{
						String side = (entry.getPositionAmt().doubleValue() < 0 ? "SHORT" : "LONG");
						BigDecimal price = entry.getEntryPrice();
						BigDecimal qty = entry.getPositionAmt().abs();

						Order tpOrder = PositionService.getTpOrder(symbol.getName(), side);
						if (tpOrder != null)
						{
							BigDecimal tpQty = tpOrder.getOrigQty();
							if (qty.compareTo(tpQty) != 0)
							{
								BigDecimal tpCoef = "SHORT".equals(side) ? BigDecimal.valueOf(1 - tpPercent) : BigDecimal.valueOf(1 + tpPercent);
								BigDecimal newTpPrice = price.multiply(tpCoef).setScale(symbol.getTickSize(), RoundingMode.HALF_UP);

								tpRearrangement(symbol, side, tpOrder, newTpPrice, qty);
							}
						}

						Order slOrder = PositionService.getSlOrder(symbol.getName(), side);
						if (slOrder != null)
						{
							slRearrangement(symbol);
						}

					}
				}
			}
		}
	}

	private static void tpRearrangement(Symbol symbol, String side, Order order, BigDecimal newPrice, BigDecimal newQty)
	{
		BigDecimal tpPrice = order.getPrice();
		BigDecimal tpQty = order.getOrigQty();

		if (isTpRearrangement)
		{
			info(symbol.getNameLeft() + " TP-REARRANGEMENT");
			info(String.format("[qty: %f price: %f] --> [qty: %f price: %f]", tpQty, tpPrice, newQty, newPrice));
			BeepUtils.beep2();

			// CANCEL ORDER
			RequestOptions options = new RequestOptions();
			SyncRequestClient syncRequestClient = SyncRequestClient.create(PrivateConfig.API_KEY, PrivateConfig.SECRET_KEY, options);
			syncRequestClient.cancelOrder(order.getSymbol(), order.getOrderId(), null);

			// ADD NEW ORDER
			SimpleTrader.postTprofit(symbol, side, newPrice, newQty);
		}
	}

	private static void slRearrangement(Symbol symbol)
	{
		if (isSlRearrangement)
		{
			//
		}
	}

	// ------------------------------------------------------------------------
	// LOG
	// ------------------------------------------------------------------------	

	private static final long LOG_MAXSIZE = 10000;

	private static LinkedList<String> logLines = new LinkedList<String>();

	public static String getLOG()
	{
		return StringUtils.join(logLines, "\n");
	}

	public static void cleanLOG()
	{
		logLines = new LinkedList<String>();
	}

	public static void log(String type, String msg)
	{
		String datetime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		String text = String.format("%-19s : %s : %s", datetime, type, msg);

		logLines.add(text);
		if (logLines.size() > LOG_MAXSIZE)
		{
			logLines.removeFirst();
		}

		notifyAllLogObservers();
	}

	public static void info(String msg)
	{
		log("INFO", msg);
	}

	public static void warn(String msg)
	{
		log("WARN", msg);
	}

	public static void error(String msg)
	{
		log("ERROR", msg);
	}

	// ------------------------------------------------------------------------	

	private static List<BotListener> observers = new ArrayList<BotListener>();

	public static void attachRefreshObserver(BotListener observer)
	{
		observers.add(observer);
	}

	public static void deattachRefreshObserver(BotListener observer)
	{
		observers.remove(observer);
	}

	public static void notifyAllLogObservers()
	{
		for (BotListener observer : observers)
		{
			observer.onBotUpdate();
		}
	}

}
