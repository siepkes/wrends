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
 * Copyright 2006-2009 Sun Microsystems, Inc.
 * Portions Copyright 2013-2016 ForgeRock AS.
 */
package org.opends.server.types;

import static org.opends.messages.UtilityMessages.*;
import static org.opends.server.util.StaticUtils.*;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.schema.AttributeType;
import org.opends.server.util.StaticUtils;

/**
 * This class defines a data structure for holding configuration
 * information to use when performing an LDIF export.
 */
@org.opends.server.types.PublicAPI(
     stability=org.opends.server.types.StabilityLevel.VOLATILE,
     mayInstantiate=true,
     mayExtend=false,
     mayInvoke=true)
public final class LDIFExportConfig extends OperationConfig
  implements Closeable
{
  /** Indicates whether the data should be compressed as it is written. */
  private boolean compressData;
  /** Indicates whether the data should be encrypted as it is written. */
  private boolean encryptData;
  /** Indicates whether to generate a cryptographic hash of the data as it is written. */
  private boolean hashData;
  /** Indicates whether to include the objectclasses in the entries written in the export. */
  private boolean includeObjectClasses;
  /** Indicates whether to include operational attributes in the export. */
  private boolean includeOperationalAttributes;
  /** Indicates whether to include virtual attributes in the export. */
  private boolean includeVirtualAttributes;
  /** Indicates whether to invoke LDIF export plugins on entries being exported. */
  private boolean invokeExportPlugins;

  /**
   * Indicates whether to digitally sign the hash when the export is complete.
   */
  private boolean signHash;

  /** Indicates whether to include attribute types (i.e., names) only or both types and values. */
  private boolean typesOnly;

  /** The path to the LDIF file that should be written. */
  private final String ldifFile;
  /** The buffered writer to which the LDIF data should be written. */
  private BufferedWriter writer;
  /** The output stream to which the LDIF data should be written. */
  private OutputStream ldifOutputStream;

  /**
   * The behavior that should be used when writing an LDIF file and a file with
   * the same name already exists.
   */
  private final ExistingFileBehavior existingFileBehavior;

  /** The column number at which long lines should be wrapped. */
  private int wrapColumn;

  /** The set of base DNs to exclude from the export. */
  private List<DN> excludeBranches;
  /** The set of base DNs to include from the export. */
  private List<DN> includeBranches;

  /** The set of search filters for entries to exclude from the export. */
  private List<SearchFilter> excludeFilters;
  /** The set of search filters for entries to include in the export. */
  private List<SearchFilter> includeFilters;

  /** The set of attribute types that should be excluded from the export. */
  private Set<AttributeType> excludeAttributes;
  /** The set of attribute types that should be included in the export. */
  private Set<AttributeType> includeAttributes;

  /**
   * Creates a new LDIF export configuration that will write to the
   * specified LDIF file.
   *
   * @param  ldifFile              The path to the LDIF file to
   *                               export.
   * @param  existingFileBehavior  Indicates how to proceed if the
   *                               specified file already exists.
   */
  public LDIFExportConfig(String ldifFile,
                          ExistingFileBehavior existingFileBehavior)
  {
    this.ldifFile                = ldifFile;
    this.existingFileBehavior    = existingFileBehavior;
    ldifOutputStream             = null;

    excludeBranches              = new ArrayList<>();
    includeBranches              = new ArrayList<>();
    excludeFilters               = new ArrayList<>();
    includeFilters               = new ArrayList<>();
    compressData                 = false;
    encryptData                  = false;
    hashData                     = false;
    includeObjectClasses         = true;
    includeOperationalAttributes = true;
    includeVirtualAttributes     = false;
    invokeExportPlugins          = false;
    signHash                     = false;
    typesOnly                    = false;
    writer                       = null;
    excludeAttributes            = new HashSet<>();
    includeAttributes            = new HashSet<>();
    wrapColumn                   = -1;
  }

  /**
   * Creates a new LDIF export configuration that will write to the
   * provided output stream.
   *
   * @param  ldifOutputStream  The output stream to which the LDIF
   *                           data should be written.
   */
  public LDIFExportConfig(OutputStream ldifOutputStream)
  {
    this.ldifOutputStream        = ldifOutputStream;
    ldifFile                     = null;
    existingFileBehavior         = ExistingFileBehavior.FAIL;

    excludeBranches              = new ArrayList<>();
    includeBranches              = new ArrayList<>();
    excludeFilters               = new ArrayList<>();
    includeFilters               = new ArrayList<>();
    compressData                 = false;
    encryptData                  = false;
    hashData                     = false;
    includeObjectClasses         = true;
    includeOperationalAttributes = true;
    includeVirtualAttributes     = false;
    invokeExportPlugins          = false;
    signHash                     = false;
    typesOnly                    = false;
    writer                       = null;
    excludeAttributes            = new HashSet<>();
    includeAttributes            = new HashSet<>();
    wrapColumn                   = -1;
  }

  /**
   * Retrieves the writer that should be used to write the LDIF data.
   * If compression or encryption are to be used, then they must be
   * enabled before the first call to this method.
   *
   * @return  The writer that should be used to write the LDIF data.
   *
   * @throws  IOException  If a problem occurs while preparing the
   *                       writer.
   */
  public BufferedWriter getWriter()
         throws IOException
  {
    if (writer == null)
    {
      if (ldifOutputStream == null)
      {
        File f = new File(ldifFile);
        boolean mustSetPermissions = false;

        switch (existingFileBehavior)
        {
        case APPEND:
          // Create new file if it doesn't exist ensuring that we can
          // set its permissions.
          if (!f.exists())
          {
            f.createNewFile();
            mustSetPermissions = true;
          }
          ldifOutputStream = new FileOutputStream(ldifFile, true);
          break;
        case OVERWRITE:
          // Create new file if it doesn't exist ensuring that we can
          // set its permissions.
          if (!f.exists())
          {
            f.createNewFile();
            mustSetPermissions = true;
          }
          ldifOutputStream = new FileOutputStream(ldifFile, false);
          break;
        case FAIL:
          if (f.exists())
          {
            LocalizableMessage message = ERR_LDIF_FILE_EXISTS.get(ldifFile);
            throw new IOException(message.toString());
          }
          // Create new file ensuring that we can set its permissions.
          f.createNewFile();
          mustSetPermissions = true;
          ldifOutputStream = new FileOutputStream(ldifFile);
          break;
        }

        if (mustSetPermissions)
        {
          try
          {
            // Ignore
            FilePermission.setSafePermissions(f, 0600);
          }
          catch (Exception e)
          {
            // The file could not be created with the correct permissions.
            LocalizableMessage message = WARN_EXPORT_LDIF_SET_PERMISSION_FAILED
                .get(f, stackTraceToSingleLineString(e));
            throw new IOException(message.toString());
          }
        }
      }

      // See if we should compress the output.
      OutputStream outputStream;
      if (compressData)
      {
        outputStream = new GZIPOutputStream(ldifOutputStream);
      }
      else
      {
        outputStream = ldifOutputStream;
      }


      // See if we should encrypt the output.
      if (encryptData)
      {
        // FIXME -- To be implemented: See OPENDJ-448.
      }


      // Create the writer.
      writer = new BufferedWriter(new OutputStreamWriter(outputStream));
    }

    return writer;
  }

  /**
   * Indicates whether the LDIF export plugins should be invoked for
   * entries as they are exported.
   *
   * @return  <CODE>true</CODE> if LDIF export plugins should be
   *          invoked for entries as they are exported, or
   *          <CODE>false</CODE> if not.
   */
  public boolean invokeExportPlugins()
  {
    return invokeExportPlugins;
  }

  /**
   * Specifies whether the LDIF export plugins should be invoked for
   * entries as they are exported.
   *
   * @param  invokeExportPlugins  Specifies whether the LDIF export
   *                              plugins should be invoked for
   *                              entries as they are exported.
   */
  public void setInvokeExportPlugins(boolean invokeExportPlugins)
  {
    this.invokeExportPlugins = invokeExportPlugins;
  }

  /**
   * Specifies whether the LDIF data should be compressed as it is
   * written.  If compression should be used, then this must be set
   * before calling <CODE>getWriter</CODE> for the first time.
   *
   * @param  compressData  Indicates whether the LDIF data should be
   *                       compressed as it is written.
   */
  public void setCompressData(boolean compressData)
  {
    this.compressData = compressData;
  }



  /**
   * Indicates whether the LDIF data should be encrypted as it is
   * written.
   *
   * @return  <CODE>true</CODE> if the LDIF data should be encrypted
   *          as it is written, or <CODE>false</CODE> if not.
   */
  public boolean encryptData()
  {
    return encryptData;
  }



  /**
   * Specifies whether the LDIF data should be encrypted as it is
   * written.  If encryption should be used, then this must be set
   * before calling <CODE>getWriter</CODE> for the first time.
   *
   * @param  encryptData  Indicates whether the LDIF data should be
   *                      encrypted as it is written.
   */
  public void setEncryptData(boolean encryptData)
  {
    this.encryptData = encryptData;
  }



  /**
   * Indicates whether to generate a cryptographic hash of the data
   * that is written.
   *
   * @return  <CODE>true</CODE> if a hash should be generated as the
   *          data is written, or <CODE>false</CODE> if not.
   */
  public boolean hashData()
  {
    return hashData;
  }



  /**
   * Specifies whether to generate a cryptographic hash of the data
   * that is written.  If hashing is to be used, then this must be set
   * before calling <CODE>getWriter</CODE> for the first time.
   *
   * @param  hashData  Indicates whether to generate a hash of the
   *                   data as it is written.
   */
  public void setHashData(boolean hashData)
  {
    this.hashData = hashData;
  }



  /**
   * Indicates whether to sign the cryptographic hash of the data that
   * is written when the export is complete.
   *
   * @return  <CODE>true</CODE> if the hash should be signed when the
   *          export is complete, or <CODE>false</CODE> if not.
   */
  public boolean signHash()
  {
    return signHash;
  }



  /**
   * Specifies whether to sign the cryptographic hash of the data that
   * is written when the export is complete.  If the export is not
   * configured to generate a hash, then this will be ignored.  If
   * hashing is to be used and the hash should be signed, then this
   * must be set before calling <CODE>getWriter</CODE> for the first
   * time.
   *
   * @param  signHash  Indicates whether to generate a hash of the
   *                   data as it is written.
   */
  public void setSignHash(boolean signHash)
  {
    this.signHash = signHash;
  }



  /**
   * Indicates whether the LDIF generated should include attribute
   * types (i.e., attribute names) only or both attribute types and
   * values.
   *
   * @return  <CODE>true</CODE> if only attribute types should be
   *          included in the resulting LDIF, or <CODE>false</CODE> if
   *          both types and values should be included.
   */
  public boolean typesOnly()
  {
    return typesOnly;
  }

  /**
   * Specifies whether the LDIF generated should include attribute
   * types (i.e., attribute names) only or both attribute types and
   * values.
   *
   * @param  typesOnly  Specifies whether the LDIF generated should
   *                    include attribute types only or both attribute
   *                    types and values.
   */
  public void setTypesOnly(boolean typesOnly)
  {
    this.typesOnly = typesOnly;
  }

  /**
   * Retrieves the column at which long lines should be wrapped.
   *
   * @return  The column at which long lines should be wrapped, or a
   *          value less than or equal to zero to indicate that no
   *          wrapping should be performed.
   */
  public int getWrapColumn()
  {
    return wrapColumn;
  }

  /**
   * Specifies the column at which long lines should be wrapped.  A
   * value less than or equal to zero indicates that no wrapping
   * should be performed.
   *
   * @param  wrapColumn  The column at which long lines should be
   *                     wrapped.
   */
  public void setWrapColumn(int wrapColumn)
  {
    this.wrapColumn = wrapColumn;
  }

  /**
   * Retrieves the set of base DNs that specify the set of entries to
   * exclude from the export.  The list that is returned may be
   * altered by the caller.
   *
   * @return  The set of base DNs that specify the set of entries to
   *          exclude from the export.
   */
  public List<DN> getExcludeBranches()
  {
    return excludeBranches;
  }

  /**
   * Specifies the set of base DNs that specify the set of entries to
   * exclude from the export.
   *
   * @param  excludeBranches  The set of base DNs that specify the set
   *                          of entries to exclude from the export.
   */
  public void setExcludeBranches(List<DN> excludeBranches)
  {
    if (excludeBranches != null)
    {
      this.excludeBranches = excludeBranches;
    }
    else
    {
      this.excludeBranches = new ArrayList<>(0);
    }
  }

  /**
   * Retrieves the set of base DNs that specify the set of entries to
   * include in the export.  The list that is returned may be altered
   * by the caller.
   *
   * @return  The set of base DNs that specify the set of entries to
   *          include in the export.
   */
  public List<DN> getIncludeBranches()
  {
    return includeBranches;
  }

  /**
   * Specifies the set of base DNs that specify the set of entries to
   * include in the export.
   *
   * @param  includeBranches  The set of base DNs that specify the set
   *                          of entries to include in the export.
   */
  public void setIncludeBranches(List<DN> includeBranches)
  {
    if (includeBranches != null)
    {
      this.includeBranches = includeBranches;
    }
    else
    {
      this.includeBranches = new ArrayList<>(0);
    }
  }

  /**
   * Indicates whether the set of objectclasses should be included in
   * the entries written to LDIF.
   *
   * @return  <CODE>true</CODE> if the set of objectclasses should be
   *          included in the entries written to LDIF, or
   *          <CODE>false</CODE> if not.
   */
  public boolean includeObjectClasses()
  {
    return includeObjectClasses;
  }

  /**
   * Indicates whether the set of operational attributes should be
   * included in the export.
   *
   * @return  <CODE>true</CODE> if the set of operational attributes
   *          should be included in the export.
   */
  public boolean includeOperationalAttributes()
  {
    return includeOperationalAttributes;
  }

  /**
   * Specifies whether the  objectclasss attribute should be
   * included in the export.
   *
   * @param  includeObjectClasses  Specifies whether the
   *                                objectclass attribute
   *                                should be included in the
   *                                export.
   */
  public void setIncludeObjectClasses(boolean includeObjectClasses)
  {
    this.includeObjectClasses = includeObjectClasses;
  }

  /**
   * Specifies whether the set of operational attributes should be
   * included in the export.
   *
   * @param  includeOperationalAttributes  Specifies whether the set
   *                                       of operational attributes
   *                                       should be included in the
   *                                       export.
   */
  public void setIncludeOperationalAttributes(
                   boolean includeOperationalAttributes)
  {
    this.includeOperationalAttributes = includeOperationalAttributes;
  }

  /**
   * Indicates whether virtual attributes should be included in the
   * export.
   *
   * @return  {@code true} if virtual attributes should be included in
   *          the export, or {@code false} if not.
   */
  public boolean includeVirtualAttributes()
  {
    return includeVirtualAttributes;
  }

  /**
   * Specifies whether virtual attributes should be included in the
   * export.
   *
   * @param  includeVirtualAttributes  Specifies whether virtual
   *                                   attributes should be included
   *                                   in the export.
   */
  public void setIncludeVirtualAttributes(
                   boolean includeVirtualAttributes)
  {
    this.includeVirtualAttributes = includeVirtualAttributes;
  }

  /**
   * Retrieves the set of attributes that should be excluded from the
   * entries written to LDIF.  The set that is returned may be altered
   * by the caller.
   *
   * @return  The set of attributes that should be excluded from the
   *          entries written to LDIF.
   */
  public Set<AttributeType> getExcludeAttributes()
  {
    return excludeAttributes;
  }

  /**
   * Specifies the set of attributes that should be excluded from the
   * entries written to LDIF.
   *
   * @param  excludeAttributes  The set of attributes that should be
   *                            excluded from the entries written to
   *                            LDIF.
   */
  public void setExcludeAttributes(
                   Set<AttributeType> excludeAttributes)
  {
    if (excludeAttributes != null)
    {
      this.excludeAttributes = excludeAttributes;
    }
    else
    {
      this.excludeAttributes = new HashSet<>(0);
    }
  }

  /**
   * Retrieves the set of attributes that should be included in the
   * entries written to LDIF.  The set that is returned may be altered
   * by the caller.
   *
   * @return  The set of attributes that should be included in the
   *          entries written to LDIF.
   */
  public Set<AttributeType> getIncludeAttributes()
  {
    return includeAttributes;
  }

  /**
   * Specifies the set of attributes that should be included in the
   * entries written to LDIF.
   *
   * @param  includeAttributes  The set of attributes that should be
   *                            included in the entries written to
   *                            LDIF.
   */
  public void setIncludeAttributes(Set<AttributeType> includeAttributes)
  {
    if (includeAttributes != null)
    {
      this.includeAttributes = includeAttributes;
    }
    else
    {
      this.includeAttributes = new HashSet<>(0);
    }
  }

  /**
   * Indicates whether the specified attribute should be included in
   * the entries written to LDIF.
   *
   * @param  attributeType  The attribute type for which to make the
   *                        determination.
   *
   * @return  <CODE>true</CODE> if the specified attribute should be
   *          included in the entries written to LDIF, or
   *          <CODE>false</CODE> if not.
   */
  public boolean includeAttribute(AttributeType attributeType)
  {
    return (excludeAttributes.isEmpty()
              || !excludeAttributes.contains(attributeType))
        && (includeAttributes.isEmpty()
              || includeAttributes.contains(attributeType));
  }

  /**
   * Retrieves the set of search filters that should be used to
   * determine which entries to exclude from the LDIF.  The list that
   * is returned may be altered by the caller.
   *
   * @return  The set of search filters that should be used to
   *          determine which entries to exclude from the LDIF.
   */
  public List<SearchFilter> getExcludeFilters()
  {
    return excludeFilters;
  }

  /**
   * Specifies the set of search filters that should be used to
   * determine which entries to exclude from the LDIF.
   *
   * @param  excludeFilters  The set of search filters that should be
   *                         used to determine which entries to
   *                         exclude from the LDIF.
   */
  public void setExcludeFilters(List<SearchFilter> excludeFilters)
  {
    if (excludeFilters != null)
    {
      this.excludeFilters = excludeFilters;
    }
    else
    {
      this.excludeFilters = new ArrayList<>(0);
    }
  }

  /**
   * Retrieves the set of search filters that should be used to
   * determine which entries to include in the LDIF.  The list that is
   * returned may be altered by the caller.
   *
   * @return  The set of search filters that should be used to
   *          determine which entries to include in the LDIF.
   */
  public List<SearchFilter> getIncludeFilters()
  {
    return includeFilters;
  }

  /**
   * Specifies the set of search filters that should be used to
   * determine which entries to include in the LDIF.
   *
   * @param  includeFilters  The set of search filters that should be
   *                         used to determine which entries to
   *                         include in the LDIF.
   */
  public void setIncludeFilters(List<SearchFilter> includeFilters)
  {
    if (includeFilters != null)
    {
      this.includeFilters = includeFilters;
    }
    else
    {
      this.includeFilters = new ArrayList<>(0);
    }
  }

  /**
   * Indicates whether the specified entry should be included in the
   * export based on the configured set of include and exclude
   * filters.
   *
   * @param  entry  The entry for which to make the determination.
   *
   * @return  <CODE>true</CODE> if the specified entry should be
   *          included in the export, or <CODE>false</CODE> if not.
   *
   * @throws  DirectoryException  If there is a problem with any of
   *                              the search filters used to make the
   *                              determination.
   */
  public boolean includeEntry(Entry entry)
         throws DirectoryException
  {
    DN dn = entry.getName();
    if (! excludeBranches.isEmpty())
    {
      for (DN excludeBranch : excludeBranches)
      {
        if (excludeBranch.isSuperiorOrEqualTo(dn))
        {
          return false;
        }
      }
    }

    checkIncludeBranches: if (! includeBranches.isEmpty())
    {
      for (DN includeBranch : includeBranches)
      {
        if (includeBranch.isSuperiorOrEqualTo(dn))
        {
          break checkIncludeBranches;
        }
      }

      return false;
    }

    if (! excludeFilters.isEmpty())
    {
      for (SearchFilter filter : excludeFilters)
      {
        if (filter.matchesEntry(entry))
        {
          return false;
        }
      }
    }

    if (! includeFilters.isEmpty())
    {
      for (SearchFilter filter : includeFilters)
      {
        if (filter.matchesEntry(entry))
        {
          return true;
        }
      }
      return false;
    }

    return true;
  }

  /** Closes any resources that this export config might have open. */
  @Override
  public void close()
  {
    // FIXME -- Need to add code to generate a signed hash of the LDIF content.
    StaticUtils.close(writer);
  }
}
