package tp1.api.service.soap;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import tp1.api.FileDelta;
import tp1.api.FileInfo;

import java.util.List;

@WebService(serviceName = SoapDirectory.NAME, targetNamespace = SoapDirectory.NAMESPACE, endpointInterface = SoapDirectory.INTERFACE)
public interface SoapDirectory {

    String NAME = "dir";
    String NAMESPACE = "http://sd2122";
    String INTERFACE = "tp1.api.service.soap.SoapDirectory";


    @WebMethod
    FileInfo writeFile(String filename, byte[] data, String userId, String password) throws DirectoryException;

    @WebMethod
    void deleteFile(String filename, String userId, String password) throws DirectoryException;

    @WebMethod
    void shareFile(String filename, String userId, String userIdShare, String password) throws DirectoryException;

    @WebMethod
    void unshareFile(String filename, String userId, String userIdShare, String password) throws DirectoryException;

    @WebMethod
    byte[] getFile(String filename, String userId, String accUserId, String password) throws DirectoryException;

    @WebMethod
    List<FileInfo> lsFile(String userId, String password) throws DirectoryException;

    @WebMethod
    void deleteUserFiles(String userId, String password, String token) throws DirectoryException;

    @WebMethod
    String getVersion(String version, String token) throws NotSupportedException;

    @WebMethod
    void applyDelta(String version, FileDelta delta, String token) throws NotSupportedException;

}
