package midirouting;

import axoloti.CConnection;
import axoloti.IConnection;
import axoloti.mvc.AbstractModel;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import qcmds.QCmdMemRead;
import qcmds.QCmdWriteMem;

/**
 *
 * @author jtaelman
 */
public class MidiInputRoutingTable extends AbstractModel {

    String portname;
    int vports[];
    int addr;

    private int getTableAddr() {
        return addr + 8;
    }

    void retrieve(IConnection conn, int addr) {
        this.addr = addr;
        conn.AppendToQueue(new QCmdMemRead(addr, 60, new IConnection.MemReadHandler() {
            @Override
            public void Done(ByteBuffer mem1) {
                int name_addr = mem1.getInt();
                conn.AppendToQueue(new QCmdMemRead(name_addr, 60, new IConnection.MemReadHandler() {
                    @Override
                    public void Done(ByteBuffer mem) {
                        String c = "";
                        byte b = mem.get();
                        while (b != 0) {
//                            System.out.println(String.format("%02X %c",(int)b, (char)b));
                            c += (char) b;
                            b = mem.get();
                        }
                        setPortName(c);

                        int nports = mem1.getInt();
                        conn.AppendToQueue(new QCmdMemRead(getTableAddr(), nports * 4, new IConnection.MemReadHandler() {
                            @Override
                            public void Done(ByteBuffer mem) {
                                int vports1[] = new int[nports];
                                for (int i = 0; i < nports; i++) {
                                    vports1[i] = mem.getInt();
                                    System.out.println(String.format("MidiInputRouting %s:%d map %08X ", getPortName(), i, vports1[i]));
                                }
                                setMapping(vports1);
                            }
                        }));
                    }
                }));
            }
        }));
    }

    public void apply(IConnection conn) {
        if ((vports != null) && (vports.length != 0)) {
            byte[] b = new byte[vports.length * 4];
            ByteBuffer bb = ByteBuffer.wrap(b);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            int i = 0;
            for (int v : vports) {
                bb.putInt(v);
                System.out.println(String.format("set inputMap %s %d %08X", portname, i++, v));
            }
            conn.AppendToQueue(new QCmdWriteMem(getTableAddr(), b));
        }
    }

    public String getPortName() {
        return portname;
    }

    public void setPortName(String portname) {
        this.portname = portname;
        firePropertyChange(
                MidiInputRoutingTableController.MIRT_PORTNAME,
                null, portname);
    }

    public int[] getMapping() {
        return vports;
    }

    public void setMapping(int[] vports) {
        if (this.vports != vports) {
            this.vports = vports;
            IConnection conn = CConnection.GetConnection();
            apply(conn);
        }
        firePropertyChange(
                MidiInputRoutingTableController.MIRT_MAPPING,
                null, vports);
    }
}
