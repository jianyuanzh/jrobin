/* ============================================================
 * JRobin : Pure java implementation of RRDTool's functionality
 * ============================================================
 *
 * Project Info:  http://www.jrobin.org
 * Project Lead:  Sasa Markovic (saxon@jrobin.org);
 *
 * (C) Copyright 2003, by Sasa Markovic.
 *
 * Developers:    Sasa Markovic (saxon@jrobin.org)
 *                Arne Vandamme (cobralord@jrobin.org)
 *
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */

package org.jrobin.data;

import org.jrobin.core.ConsolFuns;
import org.jrobin.core.Util;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class used to calculate aggregated values (MIN, MAX, LAST, FIRST, AVERAGE, TOTAL and 95-PERCENTILE) for
 * the given arrays of timestamps and associated datasource values.
 */
public class Aggregator implements ConsolFuns {
	private long timestamps[], step;
	private double[] values;

	/**
	 * Constructs Aggregator object. Note that you have to supply two arrays to the constructor: an array
	 * of timestamps and an array of datasource values. Aggregator assumes that:<p>
	 * <ul>
	 * <li>these two arrays have the same length
	 * <li>the time step between consecutive timestamps is constant.
	 * </ul>
	 * @param timestamps Array of timestamps (in seconds, without milliseconds)
	 * @param values Array of corresponding datasource values
	 */
	public Aggregator(long[] timestamps, double[] values) {
		assert timestamps.length == values.length: "Incompatible timestamps/values arrays (unequal lengths)";
		assert timestamps.length >= 2: "At least two timestamps must be supplied";
		this.timestamps = timestamps;
		this.values = values;
		this.step = timestamps[1] - timestamps[0];
	}

	/**
	 * Returns an object representing all aggregated values (MIN, MAX, LAST, FIRST, AVERAGE, TOTAL)
	 * calculated from arrays supplied in the constructor.
	 * @param tStart Starting timestamp of aggregation interval
	 * @param tEnd Ending timestamp of aggregation interval
	 * @return Object containing all aggregated values.
	 */
	public Aggregates getAggregates(long tStart, long tEnd) {
		Aggregates agg = new Aggregates();
		long totalSeconds = 0;
		boolean firstFound = false;
		for (int i = 0; i < timestamps.length; i++) {
			long left = Math.max(timestamps[i] - step, tStart);
			long right = Math.min(timestamps[i], tEnd);
			long delta = right - left;
			if (delta > 0) {
				double value = values[i];
				agg.min = Util.min(agg.min, value);
				agg.max = Util.max(agg.max, value);
				if (!firstFound) {
					agg.first = value;
					firstFound = true;
				}
				agg.last = value;
				if (!Double.isNaN(value)) {
					agg.total = Util.sum(agg.total, delta * value);
					totalSeconds += delta;
				}
			}
		}
		agg.average = totalSeconds > 0 ? (agg.total / totalSeconds) : Double.NaN;
		return agg;
	}

	/**
	 * Used by ISPs which charge for bandwidth utilization on a "95th percentile" basis.<p>
	 *
	 * The 95th percentile is the highest source value left when the top 5% of a numerically sorted set
	 * of source data is discarded. It is used as a measure of the peak value used when one discounts
	 * a fair amount for transitory spikes. This makes it markedly different from the average.<p>
	 *
	 * Read more about this topic at:<p>
	 * <a href="http://www.red.net/support/resourcecentre/leasedline/percentile.php">Rednet</a> or<br>
	 * <a href="http://www.bytemark.co.uk/support/tech/95thpercentile.html">Bytemark</a>.
	 *
	 * @param tStart Starting timestamp of aggregation interval
	 * @param tEnd Ending timestamp of aggregation interval
	 * @return 95-percentile value
	 */
	public double get95Percentile(long tStart, long tEnd) {
		List valueList = new ArrayList();
		// create a list of included datasource values (different from NaN)
		for (int i = 0; i < timestamps.length; i++) {
			long left = Math.max(timestamps[i] - step, tStart);
			long right = Math.min(timestamps[i], tEnd);
			if (right > left && !Double.isNaN(values[i])) {
				valueList.add(new Double(values[i]));
			}
		}
		// create an array to work with
		int count = valueList.size();
		if (count > 1) {
			double[] valuesCopy = new double[count];
			for (int i = 0; i < count; i++) {
				valuesCopy[i] = ((Double) valueList.get(i)).doubleValue();
			}
			// sort array
			Arrays.sort(valuesCopy);
			// skip top 5% values
			count -= (int) Math.ceil(count * 0.05);
			// if we have anything left...
			if (count > 0) {
				return valuesCopy[count - 1];
			}
		}
		// not enough data available
		return Double.NaN;
	}

	/*
	public static void main(String[] args) {
		long[] t = {10, 20, 30, 40};
		double[] v = {2, Double.NaN, 3, 1};
		Aggregator agg = new Aggregator(t, v);
		System.out.println(agg.getAggregates(0, 40).dump());
	}
	*/
}
