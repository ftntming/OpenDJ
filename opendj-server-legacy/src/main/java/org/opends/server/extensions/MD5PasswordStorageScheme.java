/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2006-2008 Sun Microsystems, Inc.
 * Portions Copyright 2013-2016 ForgeRock AS.
 */
package org.opends.server.extensions;

import java.security.MessageDigest;
import java.util.Arrays;

import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.opendj.ldap.Base64;
import org.forgerock.opendj.server.config.server.MD5PasswordStorageSchemeCfg;
import org.opends.server.api.PasswordStorageScheme;
import org.forgerock.opendj.config.server.ConfigException;
import org.opends.server.core.DirectoryServer;
import org.forgerock.i18n.slf4j.LocalizedLogger;
import org.opends.server.types.*;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.ByteSequence;

import static org.opends.messages.ExtensionMessages.*;
import static org.opends.server.extensions.ExtensionsConstants.*;
import static org.opends.server.util.StaticUtils.*;

/**
 * This class defines a Directory Server password storage scheme based on the
 * MD5 algorithm defined in RFC 1321.  This is a one-way digest algorithm
 * so there is no way to retrieve the original clear-text version of the
 * password from the hashed value (although this means that it is not suitable
 * for things that need the clear-text password like DIGEST-MD5).  This
 * implementation does not perform any salting, which means that it is more
 * vulnerable to dictionary attacks than salted variants.
 */
public class MD5PasswordStorageScheme
       extends PasswordStorageScheme<MD5PasswordStorageSchemeCfg>
{
  private static final LocalizedLogger logger = LocalizedLogger.getLoggerForThisClass();

  /** The fully-qualified name of this class. */
  private static final String CLASS_NAME =
       "org.opends.server.extensions.MD5PasswordStorageScheme";

  /** The message digest that will actually be used to generate the MD5 hashes. */
  private MessageDigest messageDigest;

  /** The lock used to provide threadsafe access to the message digest. */
  private Object digestLock;

  /**
   * Creates a new instance of this password storage scheme.  Note that no
   * initialization should be performed here, as all initialization should be
   * done in the <CODE>initializePasswordStorageScheme</CODE> method.
   */
  public MD5PasswordStorageScheme()
  {
    super();
  }

  @Override
  public void initializePasswordStorageScheme(
                   MD5PasswordStorageSchemeCfg configuration)
         throws ConfigException, InitializationException
  {
    try
    {
      messageDigest = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM_MD5);
    }
    catch (Exception e)
    {
      logger.traceException(e);

      LocalizableMessage message = ERR_PWSCHEME_CANNOT_INITIALIZE_MESSAGE_DIGEST.get(
          MESSAGE_DIGEST_ALGORITHM_MD5, e);
      throw new InitializationException(message, e);
    }

    digestLock = new Object();
  }

  @Override
  public String getStorageSchemeName()
  {
    return STORAGE_SCHEME_NAME_MD5;
  }

  @Override
  public ByteString encodePassword(ByteSequence plaintext)
         throws DirectoryException
  {
    byte[] digestBytes;
    byte[] plaintextBytes = null;

    synchronized (digestLock)
    {
      try
      {
        // TODO: Can we avoid this copy?
        plaintextBytes = plaintext.toByteArray();
        digestBytes = messageDigest.digest(plaintextBytes);
      }
      catch (Exception e)
      {
        logger.traceException(e);

        LocalizableMessage message = ERR_PWSCHEME_CANNOT_ENCODE_PASSWORD.get(
            CLASS_NAME, getExceptionMessage(e));
        throw new DirectoryException(DirectoryServer.getCoreConfigManager().getServerErrorResultCode(),
                                     message, e);
      }
      finally
      {
        if (plaintextBytes != null)
        {
          Arrays.fill(plaintextBytes, (byte) 0);
        }
      }
    }

    return ByteString.valueOfUtf8(Base64.encode(digestBytes));
  }

  @Override
  public ByteString encodePasswordWithScheme(ByteSequence plaintext)
         throws DirectoryException
  {
    StringBuilder buffer = new StringBuilder();
    buffer.append('{');
    buffer.append(STORAGE_SCHEME_NAME_MD5);
    buffer.append('}');

    byte[] plaintextBytes = null;
    byte[] digestBytes;

    synchronized (digestLock)
    {
      try
      {
        // TODO: Can we avoid this copy?
        plaintextBytes = plaintext.toByteArray();
        digestBytes = messageDigest.digest(plaintextBytes);
      }
      catch (Exception e)
      {
        logger.traceException(e);

        LocalizableMessage message = ERR_PWSCHEME_CANNOT_ENCODE_PASSWORD.get(
            CLASS_NAME, getExceptionMessage(e));
        throw new DirectoryException(DirectoryServer.getCoreConfigManager().getServerErrorResultCode(),
                                     message, e);
      }
      finally
      {
        if (plaintextBytes != null)
        {
          Arrays.fill(plaintextBytes, (byte) 0);
        }
      }
    }

    buffer.append(Base64.encode(digestBytes));

    return ByteString.valueOfUtf8(buffer);
  }

  @Override
  public boolean passwordMatches(ByteSequence plaintextPassword,
                                 ByteSequence storedPassword)
  {
    byte[] plaintextPasswordBytes = null;
    ByteString userPWDigestBytes;

    synchronized (digestLock)
    {
      try
      {
        // TODO: Can we avoid this copy?
        plaintextPasswordBytes = plaintextPassword.toByteArray();
        userPWDigestBytes =
            ByteString.wrap(messageDigest.digest(plaintextPasswordBytes));
      }
      catch (Exception e)
      {
        logger.traceException(e);

        return false;
      }
      finally
      {
        if (plaintextPasswordBytes != null)
        {
          Arrays.fill(plaintextPasswordBytes, (byte) 0);
        }
      }
    }

    ByteString storedPWDigestBytes;
    try
    {
      storedPWDigestBytes =
          ByteString.wrap(Base64.decode(storedPassword.toString()).toByteArray());
    }
    catch (Exception e)
    {
      logger.traceException(e);
      logger.error(ERR_PWSCHEME_CANNOT_BASE64_DECODE_STORED_PASSWORD, storedPassword, e);
      return false;
    }

    return userPWDigestBytes.equals(storedPWDigestBytes);
  }

  @Override
  public boolean supportsAuthPasswordSyntax()
  {
    // This storage scheme does not support the authentication password syntax.
    return false;
  }

  @Override
  public ByteString encodeAuthPassword(ByteSequence plaintext)
         throws DirectoryException
  {
    LocalizableMessage message =
        ERR_PWSCHEME_DOES_NOT_SUPPORT_AUTH_PASSWORD.get(getStorageSchemeName());
    throw new DirectoryException(ResultCode.UNWILLING_TO_PERFORM, message);
  }

  @Override
  public boolean authPasswordMatches(ByteSequence plaintextPassword,
                                     String authInfo, String authValue)
  {
    // This storage scheme does not support the authentication password syntax.
    return false;
  }

  @Override
  public boolean isReversible()
  {
    return false;
  }

  @Override
  public ByteString getPlaintextValue(ByteSequence storedPassword)
         throws DirectoryException
  {
    LocalizableMessage message = ERR_PWSCHEME_NOT_REVERSIBLE.get(STORAGE_SCHEME_NAME_MD5);
    throw new DirectoryException(ResultCode.CONSTRAINT_VIOLATION, message);
  }

  @Override
  public ByteString getAuthPasswordPlaintextValue(String authInfo,
                                                  String authValue)
         throws DirectoryException
  {
    LocalizableMessage message =
        ERR_PWSCHEME_DOES_NOT_SUPPORT_AUTH_PASSWORD.get(getStorageSchemeName());
    throw new DirectoryException(ResultCode.UNWILLING_TO_PERFORM, message);
  }

  @Override
  public boolean isStorageSchemeSecure()
  {
    // MD5 may be considered reasonably secure for this purpose.
    return true;
  }
}
