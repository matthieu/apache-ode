#!/usr/bin/expect 
# Simple expect script to run the spexerciser.
proc abort { msg } {
  send_user "FAILURE: $msg\n"
  exit -1
}
log_user 0
# keep track for later...
set cwd $env(PXE_HOME)/examples/HelloWorld
cd $cwd
spawn -noecho bpelc -wsdl file:HelloWorld.wsdl file:HelloWorld.bpel
expect -re .+ {
    abort "bpelc produced unexpected output: ${expect_out(buffer)}."
} eof {
    # no op; there is supposed to be no output...
} timeout {
    abort "bpelc took too long to run."
}
if { ! [file exists "HelloWorld.cbp"]} {
  abort "bpelc did not produce expected compiled BPEL file."
}
spawn -noecho rradd -q -wsdl file:HelloWorld.wsdl hw.rr
expect -re .+ {
    abort "rradd produced unexpected output."
} eof {
    #no op; there is supposed to be no output...
} timeout {
    abort "rradd took too long to run."
}
if { ! [file exists "hw.rr"] } {
    abort "rradd did not produce the expected rr file."
}
# Too bad this has to be done; JAR deployment is b0rked, and that's what
# the descriptor is written for...
spawn -noecho rralias -q file:HelloWorld.wsdl file:examples/HelloWorld/HelloWorld.wsdl hw.rr
expect -re .+ {
    abort "rralias produced unexpected output: $expect_out(buffer)."
} eof {
    # no op; there is supposed to be no output.
} timeout {
    abort "rralias took too long to run."
}
# Again, JAR deployment forces this b0rkage.
exec cp $cwd/HelloWorld.cbp $cwd/a.cbp
spawn -noecho sarcreate -sysd pxe-system.xml -common hw.rr a.cbp hw.sar
expect -re .+ {
    abort "sarcreate produced unexpected output: $expect_out(buffer)."
} eof {
    # no op; there is supposed to be no output.
} timeout {
    abort "sarcreate took too long to run."
}
if { ! [file exists "hw.sar"] } {
    abort "sarcreate did not produce the execpted sar file."
}
# Fire up PXE.
spawn -noecho pxe -v -console
expect -timeout 30 -re ^pxe.*\r\n?Copyright.*\r\n? {
    # no op.
} timeout {
    abort "pxe took too long to output the initial header."
} eof {
    abort "pxe exited suddenly."
}
expect -timeout 100 -re ^Warning.*XSLT.*\r\n? {
    exp_continue
} -re ^WARN.*\r\n? {
    exp_continue
} -re ^ERROR.*\r\n? {
    abort "pxe encountered an internal error: $expect_out(buffer)"
} eof {
    abort "pxe exited suddenly (probably due to error)."
} -re ^INFO\.*\ Startup\ completed\.\r\n? {
    # no op; server up.
} -re ^INFO.*\r\n? {
    exp_continue    
} timeout {
    abort "pxe took too long to start up; aborting."
} 
expect_background -re ^INFO.*\r\n? {
    exp_continue
} -re ^WARN.*\r\n? {
    exp_continue
} -re ^ERROR.*\r\n? {
    abort "pxe encountered an error - $expect_out(buffer)"
} -re ^FATAL.*\r\n? {
    abort "pxe encountered a fatal error - $expect_out(buffer)"
} eof {
    abort "pxe exited suddenly.\n"
}
# clean house; there could be a lot in there...
spawn -noecho pxe-undeploy -all
expect -timeout 100 eof {
    # no op
} -re ^.+ {
    abort "pxe-undeploy produced unexpected output: $expect_out(buffer)"
} timeout {
   abort "pxe-undeploy took too long to run."
}
# deploy the SAR
spawn -noecho pxe-deploy -sar hw.sar
expect -re ^.+ {
    abort "pxe-deploy produced unexpected output: $expect_out(buffer)"
} eof {
    # no op; no output is expected.
} timeout {
    abort "pxe-deploy took too long to run"
}
spawn -noecho pxe-status
expect -re ^Domain:\ MyDomain\ \[\(\]1\ system,\ 1\ active\[\)\]\r\n? {
    exp_continue
} -re ^0:\ \ \\+\ \ HelloWorld\r\n? {
    # no op; this is what we wanted.
} timeout {
    abort "pxe-status returned $expect_out(buffer) instead of expected results."
} eof {
    abort "pxe-status returned $expect_out(buffer) instead of expecetd results."
}
spawn -noecho sendsoap http://127.0.0.1:8080/pxe/soap/helloWorld $cwd/testRequest.soap
expect -timeout 100 -re <TestPart.*>Hello\ World</TestPart>.* {
} default {
    abort "sendsoap did not return the expected response."
}
send_user "SUCCESS: HelloWorld build, deployed, and sent test message."
exit 0