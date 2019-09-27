package com.xuhe.protocol.server;

import com.xuhe.aace.common.RcvPkgNode;
import com.xuhe.aace.common.RetCode;
import com.xuhe.aace.context.AaceContext;
import com.xuhe.aace.handler.AaceHandler;
import com.xuhe.aace.packer.FieldType;
import com.xuhe.aace.packer.PackData;
import com.xuhe.aace.packer.PackException;

public class AaceCheckServer extends AaceHandler {

    public AaceCheckServer(String proxy, String interfaceName, String uri){
        this.proxy = proxy;
        this.interfaceName = interfaceName;
        this.uri = uri;
        this.init();
    }


    @Override
    protected boolean registerHandler() {
        boolean r = aaceMgr.registerHandler("AaceCheck", "check", this, "check", mode);
        System.out.println(r +"  AaceCheck.check");
        return true;
    }

    public int check(RcvPkgNode pkgNode, AaceContext ctx) {
        System.out.println(pkgNode + "7888888888888888 =================");
        int status;
        PackData __packer = new PackData();
        __packer.resetInBuff(pkgNode.getMessage());
        try {
            byte __num = __packer.unpackByte();
            FieldType __field;
            if(__num < 1) throw new PackException(PackData.PACK_LENGTH_ERROR, "PACK_LENGTH_ERROR");
            __field = __packer.unpackFieldType();
            if(!PackData.matchType(__field.baseType, PackData.FT_NUMBER)) throw new PackException(PackData.PACK_TYPEMATCH_ERROR, "PACK_TYPEMATCH_ERROR");
            status = __packer.unpackInt();
        } catch (PackException e) {
            return PackData.PACK_INVALID;
        }
        return RetCode.RET_NORESPONSE;
    }
}
