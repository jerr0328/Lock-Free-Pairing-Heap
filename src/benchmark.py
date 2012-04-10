
import subprocess, time
import os

def runCommand(command,out=None,err=None):

	p = subprocess.Popen(command,stdout=out,stderr=err,shell=True)
	p.wait()
	
	
def benchmarkCommand(command,out=None,err=None):
	
	begin = time.time()
	runCommand(command,out,err)
	return time.time() - begin
	
#Benchmark the PairingHeapMain class with specified minimum and maximum heap sizes (in MB)
def testPairingHeap(mini=64,maxi=5632):
	
	command = "java -server -Xms"+str(mini)+"m -Xmx"+str(maxi)+"m PairingHeapMain"
	print command
	return benchmarkCommand(command)
	
if __name__ == "__main__":
	
	print str(testPairingHeap(64,5632))
