package com.azvasa.collector;

import com.vmware.vim25.*;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.PerformanceManager;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by mrugen on 4/24/15.
 */
public class Collector {

    static ArrayList<String> xco = new ArrayList<String>();
    static ArrayList<Long> ycoCPU = new ArrayList<Long>();
    static ArrayList<Long> ycoMemory = new ArrayList<Long>();


    public static void main(String[] args) throws Exception
    {

        URL url = new URL("https://130.65.132.109/sdk");
        ServiceInstance si = new ServiceInstance(url, "administrator", "12!@qwQW", true);

        String vmname = "VM-CLI-09";

        VirtualMachine vm = (VirtualMachine) new InventoryNavigator(
                si.getRootFolder()).searchManagedEntity(
                "VirtualMachine", vmname);

        if(vm == null)
        {
            System.out.println("Virtual Machine " + vmname
                    + " cannot be found.");
            si.getServerConnection().logout();
            return;
        }

        PerformanceManager perfMgr = si.getPerformanceManager();

        int perfInterval = 1800; // 30 minutes for PastWeek


        // retrieve all the available perf metrics for vm
        PerfMetricId[] pmis = perfMgr.queryAvailablePerfMetric(
                vm, null, null, perfInterval);

        for(PerfMetricId id : pmis)
        {
            id.setInstance("");
        }


        Calendar curTime = si.currentTime();

        PerfQuerySpec qSpec = new PerfQuerySpec();
        qSpec.setEntity(vm.getRuntime().getHost());
        //metricIDs must be provided, or InvalidArgumentFault
        qSpec.setMetricId(pmis);
        qSpec.setFormat("normal"); //optional since it's default
        qSpec.setIntervalId(perfInterval);

        Calendar startTime = (Calendar) curTime.clone();
        startTime.roll(Calendar.DATE, -1);
        System.out.println("start:" + startTime.getTime());
        qSpec.setStartTime(startTime);

        Calendar endTime = (Calendar) curTime.clone();
        endTime.roll(Calendar.DATE, 0);
        System.out.println("end:" + endTime.getTime());
        qSpec.setEndTime(endTime);



        PerfCompositeMetric pv = perfMgr.queryPerfComposite(qSpec);
        if(pv != null)
        {
            printPerfMetric(pv.getEntity());
            PerfEntityMetricBase[] pembs = pv.getChildEntity();
            for(int i=0; pembs!=null && i< pembs.length; i++)
            {
                printPerfMetric(pembs[i]);
            }
        }

        System.out.println("**********************Stats****************** ");
        System.out.println("time :");
        for(String s : xco)
            System.out.print(s + " ");

        System.out.println("");
        System.out.println("CPU Usage : ");

        for(Long s : ycoCPU)
            System.out.print(s + " ");

        System.out.println("");
        System.out.println("Memory Usage ");
        for(Long s : ycoMemory)
            System.out.print(s + " ");

        si.getServerConnection().logout();
    }

    static void printPerfMetric(PerfEntityMetricBase val)
    {
        if(val instanceof PerfEntityMetric)
        {
            printPerfMetric((PerfEntityMetric)val);
        }
    }

    static void printPerfMetric(PerfEntityMetric pem)
    {
        PerfMetricSeries[] vals = pem.getValue();
        PerfSampleInfo[]  infos = pem.getSampleInfo();

        for(int i=0; infos!=null && i<infos.length; i++)
        {
            xco.add(infos[i].getTimestamp().getTime().toString());
        }

        for(int j=0; vals!=null && j<vals.length; ++j)
        {
            if(vals[j].getId().getCounterId()==2)
            {
                if(vals[j] instanceof PerfMetricIntSeries)
                {
                    PerfMetricIntSeries val = (PerfMetricIntSeries) vals[j];
                    long[] longs = val.getValue();
                    for(int k=0; k<longs.length; k++)
                    {
                        System.out.print(longs[k] + " ");
                        ycoCPU.add(longs[k]);
                    }
                }
            }
        }


        for(int j=0; vals!=null && j<vals.length; ++j)
        {
            if(vals[j].getId().getCounterId()==24)
            {
                if(vals[j] instanceof PerfMetricIntSeries)
                {
                    PerfMetricIntSeries val = (PerfMetricIntSeries) vals[j];
                    long[] longs = val.getValue();
                    for(int k=0; k<longs.length; k++)
                    {
                        System.out.print(longs[k] + " ");
                        ycoMemory.add(longs[k]);
                    }
                }
            }
        }
    }
}
