<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>
  <xsl:key name="ACCOUNT-by-ACCOUNT_OBJ" match="invoice/PIN_FLD_AR_ITEMS" use="substring-before(substring-after(substring-after(PIN_FLD_ACCOUNT_OBJ,' '),' '),' ')"/>
  <xsl:key name="SERVICE-by-SERVICE_OBJ" match="invoice/PIN_FLD_AR_ITEMS" use="substring-before(substring-after(substring-after(PIN_FLD_SERVICE_OBJ,' '),' '),' ')"/>
  <xsl:key name="NETWORK-by-SERVICE_OBJ" match="invoice/TM_FLD_CUST_SRV_NW_PROFILE/PIN_FLD_SERVICES" use="substring-before(substring-after(substring-after(PIN_FLD_SERVICE_OBJ,' '),' '),' ')"/>
  <xsl:variable name="primary-currency-id">
    <xsl:value-of select="/invoice/PIN_FLD_BILLINFO/PIN_FLD_CURRENCY"/>
  </xsl:variable>
  <xsl:variable name="totalPayments">
    <xsl:choose>
      <xsl:when test="sum(invoice/PIN_FLD_OTHER_ITEMS[contains(PIN_FLD_NAME,'Payment')]/PIN_FLD_ITEM_TOTAL) != ''">
        <xsl:value-of select="sum(invoice/PIN_FLD_OTHER_ITEMS[contains(PIN_FLD_NAME,'Payment')]/PIN_FLD_ITEM_TOTAL)"/>
      </xsl:when>
      <xsl:otherwise>0.00</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="totalCreditAdj">
    <xsl:value-of select="sum(invoice/PIN_FLD_OTHER_ITEMS[contains(PIN_FLD_NAME,'Adjustment')]/PIN_FLD_EVENTS[not(contains(PIN_FLD_DESCR,'5 Sen Government Rounding'))]/PIN_FLD_TOTAL[contains(PIN_FLD_AMOUNT,'-')]/PIN_FLD_AMOUNT)"/>
  </xsl:variable>
  <xsl:variable name="totalDebitAdj">
    <xsl:value-of select="sum(invoice/PIN_FLD_OTHER_ITEMS[contains(PIN_FLD_NAME,'Adjustment')]/PIN_FLD_EVENTS[not(contains(PIN_FLD_DESCR,'5 Sen Government Rounding'))]/PIN_FLD_TOTAL [not ( contains(PIN_FLD_AMOUNT,'-'))]/PIN_FLD_AMOUNT )"/>
  </xsl:variable>
  <xsl:variable name="AcctDebitTax">
    <xsl:choose>
      <xsl:when test="invoice/PIN_FLD_BILLINFO/PIN_FLD_DB_AR_TAX_AMT != ''">
        <xsl:value-of select="invoice/PIN_FLD_BILLINFO/PIN_FLD_DB_AR_TAX_AMT"/>
      </xsl:when>
      <xsl:otherwise>0.00</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="totalTax">
    <xsl:value-of select="sum(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_ITEM_OBJ,'cycle_tax')]/PIN_FLD_ITEM_TOTAL)"/>
  </xsl:variable>
  <xsl:variable name="usgSMSIPTV">
    <xsl:value-of select="sum(/invoice/PIN_FLD_AR_ITEMS/PIN_FLD_EVENTS[contains(PIN_FLD_ITEM_OBJ,'usage') or contains(PIN_FLD_ITEM_OBJ,'iptv') or contains(PIN_FLD_ITEM_OBJ,'burstable')]/PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id or PIN_FLD_RESOURCE_ID =1000107]/PIN_FLD_AMOUNT)"/>
  </xsl:variable>
  <xsl:variable name="usgBURST">
    <xsl:value-of select="sum(/invoice/PIN_FLD_AR_ITEMS/PIN_FLD_EVENTS[contains(PIN_FLD_ITEM_OBJ,'burstable') and contains(PIN_FLD_DESCR, 'BURST')]/PIN_FLD_TOTAL[@elem = $primary-currency-id]/PIN_FLD_AMOUNT)"/>
  </xsl:variable>
  <xsl:variable name="totalAllUSG">
    <xsl:value-of select="format-number($usgSMSIPTV + $usgBURST, '###,###,###,##0.00')"/>
  </xsl:variable>
  <xsl:variable name="totalRoundingAdj">
    <xsl:choose>
      <xsl:when test="invoice/PIN_FLD_BILLINFO/PIN_FLD_DELTA_DUE != ''">
        <xsl:value-of select="invoice/PIN_FLD_BILLINFO/PIN_FLD_DELTA_DUE"/>
      </xsl:when>
      <xsl:otherwise>0.00</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="currAdj">
    <xsl:choose>
      <xsl:when test="invoice/PIN_FLD_BILLINFO/TM_FLD_CURR_ADJ_AMT != ''">
        <xsl:value-of select="format-number(invoice/PIN_FLD_BILLINFO/TM_FLD_CURR_ADJ_AMT, '###,###,###,##0.00')"/>
      </xsl:when>
      <xsl:otherwise>0.00</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="prevAdj_DB">
    <xsl:choose>
      <xsl:when test="invoice/PIN_FLD_BILLINFO/TM_FLD_PREV_ADJ_DB_AMT != ''">
        <xsl:value-of select="invoice/PIN_FLD_BILLINFO/TM_FLD_PREV_ADJ_DB_AMT"/>
      </xsl:when>
      <xsl:otherwise>0.00</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="prevAdj_CR">
    <xsl:choose>
      <xsl:when test="invoice/PIN_FLD_BILLINFO/TM_FLD_PREV_ADJ_CR_AMT != ''">
        <xsl:value-of select="invoice/PIN_FLD_BILLINFO/TM_FLD_PREV_ADJ_CR_AMT"/>
      </xsl:when>
      <xsl:otherwise>0.00</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="prevAdj">
    <xsl:value-of select="$prevAdj_CR + $prevAdj_DB"/>
  </xsl:variable>
  <xsl:variable name="totalDeposit">
    <xsl:choose>
      <xsl:when test="invoice/PIN_FLD_ACCTINFO/PIN_FLD_CURRENT_BAL != 0">
        <xsl:value-of select="format-number(invoice/PIN_FLD_ACCTINFO/PIN_FLD_CURRENT_BAL *-1, '###,###,###,##0.00')"/>
      </xsl:when>
      <xsl:otherwise>0.00</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="totalOutstanding">
    <xsl:choose>
      <xsl:when test="contains(invoice/PIN_FLD_BILLINFO/PIN_FLD_PREVIOUS_TOTAL,'-0.00')">0.00</xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="invoice/PIN_FLD_BILLINFO/PIN_FLD_PREVIOUS_TOTAL"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="lowercase">abcdefghijklmnopqrstuvwxyz</xsl:variable>
  <xsl:variable name="uppercase">ABCDEFGHIJKLMNOPQRSTUVWXYZ</xsl:variable>
  <xsl:variable name="total_SMS">
    <xsl:value-of select="sum(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_ITEM_OBJ,'usage')]/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='IB00']/PIN_FLD_NET_QUANTITY)"/>
  </xsl:variable>
  <xsl:variable name="languagePreference">
    <xsl:value-of select="translate(/invoice/PIN_FLD_ACCTINFO/TM_FLD_BILL_LANGUAGE,$lowercase,$uppercase)"/>
  </xsl:variable>
  <xsl:variable name="taxRate">0.06</xsl:variable>
  <!-- Added by David Khaw 23 August 2012  (Offset set to 2005) -->
  <!-- yearOffsetInSecs: to offset seconds based on 01/01/$yearOffset 00:00:00 so as to make date calculation in this stylesheet faster -->
  <!-- yearOffset: year that is offset. To Change together with yearOffsetInSecs -->
  <!-- New Date Calculation Logic: -->
  <!-- 1. Take unix time as stored in DB, subtract yearOffsetInSecs -->
  <!-- 2. Subtract numSecs in Year if secs > secs in a year for that year -->
  <!-- 3. Repeat till numSecs is less than a year's worth of seconds -->
  <!-- 4. Determine daysInYear -->
  <!-- 5. Determine correct month (checks leap year) -->
  <!-- 6. Determine day (checks leap year for feb29) -->
  <xsl:variable name="yearOffsetInSecs">1104537600</xsl:variable>
  <xsl:variable name="yearOffset">2005</xsl:variable>

  <!-- START AmalinaRazif.23072013 Added DATE format for BARCODE -->
  <xsl:variable name="bc-date"><xsl:call-template name="format-unix-date-bc-date"><xsl:with-param name="unix" select="invoice/PIN_FLD_BILLINFO/PIN_FLD_END_T" /></xsl:call-template>
  </xsl:variable>
  <!-- START AmalinaRazif.23072013 Added DATE format for BARCODE -->

  <!-- START AmalinaRazif.23072013 Added ACC_TOTAL format for BARCODE -->
  <!-- <xsl:variable name="bc-acc-total"><xsl:value-of select="format-number(invoice/PIN_FLD_BILLINFO/PIN_FLD_CURRENT_TOTAL + $totalOutstanding + $totalRoundingAdj,'###########0.00')" />
  </xsl:variable> -->
  <xsl:variable name="bc-acc-total">
  <xsl:call-template name="prepend-pad">
	<xsl:with-param name="padChar" select="0" />
    <xsl:with-param name="padVar" select="format-number(invoice/PIN_FLD_BILLINFO/PIN_FLD_CURRENT_TOTAL + $totalOutstanding + $totalRoundingAdj,'#0000000.00')" />
	<xsl:with-param name="length" select="11" />
</xsl:call-template>
</xsl:variable>
<!-- END AmalinaRazif.23072013 Added ACC_TOTAL format for BARCODE -->

<!-- <xsl:variable name="bc-acc-total"><xsl:value-of select="format-number(invoice/PIN_FLD_BILLINFO/PIN_FLD_CURRENT_TOTAL + $totalOutstanding + $totalRoundingAdj,'#0000000.00')" />
</xsl:variable> -->

  <xsl:template match="/">
    <INVOICE>
      <!--  Account Information  -->
      <ACCTINFO>
        <!--  Bill Information  -->
        <ACCOUNT_NO>
          <xsl:value-of select="invoice/PIN_FLD_ACCTINFO/PIN_FLD_ACCOUNT_NO"/>
        </ACCOUNT_NO>
        <LEGACY_ACCOUNT_NO>
          <xsl:value-of select="invoice/PIN_FLD_ACCTINFO/TM_FLD_LEG_BILL_ACCT_NUM"/>
        </LEGACY_ACCOUNT_NO>
        <!-- ACCOUNT_CURRENCY><xsl:value-of select="invoice/PIN_FLD_CURRENCIES[@elem=$primary-currency-id]/PIN_FLD_BEID_STR_CODE"/></ACCOUNT_CURRENCY -->
        <NAME>
          <xsl:value-of select="translate(invoice/PIN_FLD_NAMEINFO/PIN_FLD_LAST_NAME,$lowercase,$uppercase)"/>
        </NAME>
        <ADDRESS>
          <xsl:choose>
            <xsl:when test="contains(invoice/PIN_FLD_NAMEINFO/PIN_FLD_ADDRESS,'|')">
              <xsl:if test="string-length(substring-before(translate(translate(invoice/PIN_FLD_NAMEINFO/PIN_FLD_ADDRESS,$lowercase,$uppercase),',',''),'|')) &gt; 0">
                <ADDRESS_LINE>
                  <xsl:value-of select="substring-before(translate(translate(invoice/PIN_FLD_NAMEINFO/PIN_FLD_ADDRESS,$lowercase,$uppercase),',',' '),'|')"/>
                </ADDRESS_LINE>
              </xsl:if>
              <xsl:choose>
                <xsl:when test="contains(substring-after(invoice/PIN_FLD_NAMEINFO/PIN_FLD_ADDRESS,'|'),'|')">
                  <xsl:if test="string-length(substring-before(substring-after(translate(translate(invoice/PIN_FLD_NAMEINFO/PIN_FLD_ADDRESS,$lowercase,$uppercase), ',', ''),'|'),'|'))  &gt; 0">
                    <ADDRESS_LINE>
                      <xsl:value-of select="substring-before(substring-after(translate(translate(invoice/PIN_FLD_NAMEINFO/PIN_FLD_ADDRESS,$lowercase,$uppercase), ',', ' '),'|'),'|')"/>
                    </ADDRESS_LINE>
                  </xsl:if>
                  <xsl:choose>
                    <xsl:when test="contains(substring-after(substring-after(invoice/PIN_FLD_NAMEINFO/PIN_FLD_ADDRESS,'|'),'|'),'|') ">
                      <xsl:if test="string-length(substring-before(substring-after(substring-after(translate(translate(invoice/PIN_FLD_NAMEINFO/PIN_FLD_ADDRESS,$lowercase,$uppercase),',',''),'|'),'|'),'|')) &gt; 0">
                        <ADDRESS_LINE>
                          <xsl:value-of select="substring-before(substring-after(substring-after(translate(translate(invoice/PIN_FLD_NAMEINFO/PIN_FLD_ADDRESS,$lowercase,$uppercase),',',' '),'|'),'|'),'|')"/>
                        </ADDRESS_LINE>
                      </xsl:if>
                      <xsl:choose>
                        <xsl:when test="contains(substring-after(substring-after(substring-after(invoice/PIN_FLD_NAMEINFO/PIN_FLD_ADDRESS,'|'),'|'),'|'),'|')">
                          <xsl:if test="string-length(substring-before(substring-after(substring-after(substring-after(translate(translate(invoice/PIN_FLD_NAMEINFO/PIN_FLD_ADDRESS,$lowercase,$uppercase), ',', ''),'|'),'|'),'|'),'|')) &gt; 0">
                            <ADDRESS_LINE>
                              <xsl:value-of select="substring-before(substring-after(substring-after(substring-after(translate(translate(invoice/PIN_FLD_NAMEINFO/PIN_FLD_ADDRESS,$lowercase,$uppercase), ',', ' '),'|'),'|'),'|'),'|')"/>
                            </ADDRESS_LINE>
                          </xsl:if>
                        </xsl:when>
                        <xsl:otherwise>
                          <ADDRESS_LINE>
                            <xsl:value-of select="substring-after(substring-after(substring-after(translate(translate(invoice/PIN_FLD_NAMEINFO/PIN_FLD_ADDRESS,$lowercase,$uppercase), ',', ''),'|'),'|'),'|')"/>
                          </ADDRESS_LINE>
                        </xsl:otherwise>
                      </xsl:choose>
                    </xsl:when>
                    <xsl:otherwise>
                      <ADDRESS_LINE>
                        <xsl:value-of select="substring-after(substring-after(translate(translate(invoice/PIN_FLD_NAMEINFO/PIN_FLD_ADDRESS,$lowercase,$uppercase), ',', ''),'|'),'|')"/>
                      </ADDRESS_LINE>
                    </xsl:otherwise>
                  </xsl:choose>
                </xsl:when>
                <xsl:otherwise>
                  <ADDRESS_LINE>
                    <xsl:value-of select="substring-after(translate(translate(invoice/PIN_FLD_NAMEINFO/PIN_FLD_ADDRESS,$lowercase,$uppercase), ',', ' '),'|')"/>
                  </ADDRESS_LINE>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
              <ADDRESS_LINE>
                <xsl:value-of select="translate(translate(invoice/PIN_FLD_NAMEINFO/PIN_FLD_ADDRESS,$lowercase,$uppercase),',', ' ')"/>
              </ADDRESS_LINE>
            </xsl:otherwise>
          </xsl:choose>
        </ADDRESS>
        <CITY>
          <xsl:value-of select="translate(/invoice/PIN_FLD_NAMEINFO/PIN_FLD_CITY,$lowercase,$uppercase)"/>
        </CITY>
        <STATE>
          <xsl:value-of select="translate(/invoice/PIN_FLD_NAMEINFO/PIN_FLD_STATE,$lowercase,$uppercase)"/>
        </STATE>
        <ZIP>
          <xsl:value-of select="/invoice/PIN_FLD_NAMEINFO/PIN_FLD_ZIP"/>
        </ZIP>
        <COUNTRY>
          <xsl:if test="invoice/PIN_FLD_NAMEINFO[not(contains(translate(PIN_FLD_COUNTRY,$lowercase,$uppercase),'MALAYSIA'))]">
            <xsl:value-of select="translate(invoice/PIN_FLD_NAMEINFO/PIN_FLD_COUNTRY,$lowercase,$uppercase)"/>
          </xsl:if>
        </COUNTRY>
        <REVENUE_STREAM>
          <xsl:value-of select="invoice/PIN_FLD_ACCTINFO/PIN_FLD_REVENUE_STREAM"/>
        </REVENUE_STREAM>
        <SEGMENT_CODE>
          <xsl:value-of select="invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_CODE"/>
        </SEGMENT_CODE>
        <SEGMENT_GROUP>
          <xsl:value-of select="invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP"/>
        </SEGMENT_GROUP>
		<COST_CENTER>
		<xsl:if test = "invoice/PIN_FLD_ACCTINFO/TM_FLD_COST_CENTER != ''">
				<xsl:value-of select="invoice/PIN_FLD_ACCTINFO/TM_FLD_COST_CENTER"/>
		</xsl:if>
		</COST_CENTER>
        <DEPOSIT>
          <xsl:value-of select="$totalDeposit"/>
        </DEPOSIT>
        <xsl:if test="/invoice/PIN_FLD_ACCTINFO/PIN_FLD_CREDIT_LIMIT > 0">
			<xsl:choose>
				<xsl:when test="invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP = 'CONSUMER'">
					<CREDIT_LIMIT>
						<xsl:value-of select="format-number(invoice/PIN_FLD_ACCTINFO/PIN_FLD_CREDIT_LIMIT,'###,###,###,##0.00')"/>
					</CREDIT_LIMIT>
				</xsl:when>
				<xsl:when test="invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP = 'SME'">
					<xsl:if test="invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_CODE = 'S10' or invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_CODE = 'S20' ">
						<CREDIT_LIMIT>
							<xsl:value-of select="format-number(invoice/PIN_FLD_ACCTINFO/PIN_FLD_CREDIT_LIMIT,'###,###,###,##0.00')"/>
						</CREDIT_LIMIT>
					  </xsl:if>
				</xsl:when>
			</xsl:choose>
        </xsl:if>
      </ACCTINFO>
      <BILL>
	<!-- START AmalinaRazif.23072013 Added barcode tag -->
	<BARCODE><xsl:value-of select="concat(invoice/PIN_FLD_ACCTINFO/PIN_FLD_ACCOUNT_NO,'    ',$bc-acc-total,$bc-date,invoice/PIN_FLD_ACCTINFO/PIN_FLD_REVENUE_STREAM)"/></BARCODE>
	<!-- END AmalinaRazif.23072013 Added barcode tag -->
        <BILL_CURRENCY>
          <xsl:choose>
            <xsl:when test="invoice/PIN_FLD_CURRENCIES[@elem=$primary-currency-id]/PIN_FLD_BEID_STR_CODE = 'MYR'">RM</xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="invoice/PIN_FLD_CURRENCIES[@elem=$primary-currency-id]/PIN_FLD_BEID_STR_CODE"/>
            </xsl:otherwise>
          </xsl:choose>
        </BILL_CURRENCY>
        <BILL_NO>
          <xsl:value-of select="invoice/PIN_FLD_BILLINFO/PIN_FLD_BILL_NO"/>
        </BILL_NO>
        <BILL_DATE>
          <xsl:if test="invoice/PIN_FLD_BILLINFO/PIN_FLD_END_T != ''">
            <xsl:call-template name="format-unix-date-invoice-date">
              <xsl:with-param name="unix" select="invoice/PIN_FLD_BILLINFO/PIN_FLD_END_T"/>
            </xsl:call-template>
          </xsl:if>
        </BILL_DATE>
        <BILL_DUE_DATE>
          <xsl:if test="invoice/PIN_FLD_BILLINFO/PIN_FLD_DUE_T != ''">
            <xsl:call-template name="format-unix-date-invoice-date">
              <xsl:with-param name="unix" select="invoice/PIN_FLD_BILLINFO/PIN_FLD_DUE_T"/>
            </xsl:call-template>
          </xsl:if>
        </BILL_DUE_DATE>
        <LANG_PREFERENCE>
          <xsl:value-of select="$languagePreference"/>
        </LANG_PREFERENCE>
        <ITEMIZED>
          <xsl:value-of select="invoice/PIN_FLD_ACCTINFO/TM_FLD_ITEMIZED_BILL"/>
        </ITEMIZED>
        <INCLUDE_BROCHURE></INCLUDE_BROCHURE>
        <OFFPAPER_GOV>
          <xsl:if test="invoice/PIN_FLD_BILLINFO[contains (PIN_FLD_PAYINFO_OBJ,'pukal')]">PUKAL</xsl:if>
          <xsl:if test="invoice/PIN_FLD_BILLINFO[contains(PIN_FLD_PAYINFO_OBJ,'cc') or contains(PIN_FLD_PAYINFO_OBJ,'dd')]">AUTOPAY</xsl:if>
		  <!--AmalinaRazif - Added Payment Method CBMS to be displayed in the bill -->
		  <xsl:if test="invoice/PIN_FLD_BILLINFO[contains(PIN_FLD_PAYINFO_OBJ,'tm_kps')]">CBMS</xsl:if>
        </OFFPAPER_GOV>
        <xsl:if test="/invoice/PIN_FLD_ACCTINFO/PIN_FLD_DELIVERY_PREFER = 0">
          <OFFPAPER_ACT>
            <xsl:if test="/invoice/PIN_FLD_ACCTINFO/PIN_FLD_DELIVERY_PREFER = 0">OFF</xsl:if>
          </OFFPAPER_ACT>
        </xsl:if>
        <BILL_STREAM>
          <xsl:value-of select="invoice/PIN_FLD_ACCTINFO/TM_FLD_BILL_STREAM"/>
        </BILL_STREAM>
        <BILL_TYPE>CP</BILL_TYPE>
        <BILL_MSG_SEGMENT>
          <xsl:call-template name="bill-message-conditional"/>
          <xsl:choose>
            <xsl:when test="(invoice/PIN_FLD_BILLINFO/PIN_FLD_PREVIOUS_TOTAL)>0">
              <BILL_MSG>
                <xsl:call-template name="bill-Message-Outstanding"/>
              </BILL_MSG>
              <xsl:if test="( invoice/PIN_FLD_BILLINFO/PIN_FLD_CURRENT_TOTAL)>0">
                <BILL_MSG>
                  <xsl:call-template name="bill-Message-Current"/>
                </BILL_MSG>
              </xsl:if>		  
            </xsl:when>
            <xsl:when test="(invoice/PIN_FLD_BILLINFO/PIN_FLD_CURRENT_TOTAL)>0 ">
              <BILL_MSG>
                <xsl:call-template name="bill-Message-Current"/>
              </BILL_MSG>
            </xsl:when>
			      <xsl:otherwise></xsl:otherwise>
          </xsl:choose>
		  <!-- andrew 26062014 Targeted Bill Message M2R2- For Migration -->
          <xsl:if test="(invoice/PIN_FLD_ACCTINFO/TM_FLD_BILL_STREAM)='92'">
            <BILL_MSG><xsl:call-template name="bill-Message-Announcement"/></BILL_MSG>
          </xsl:if>
		      <xsl:if test="(invoice/PIN_FLD_ACCTINFO/TM_FLD_BILL_STREAM)='82' or (invoice/PIN_FLD_ACCTINFO/TM_FLD_BILL_STREAM)='41'">
            <BILL_MSG><xsl:call-template name="bill-Message-Announcement-Second"/></BILL_MSG>
          </xsl:if>
          <!-- Ivor 15122012 Targeted Bill Message M2R2- Front Start -->
          <xsl:call-template name="bill-Message-front"><xsl:with-param name="event-list" select="/invoice/PIN_FLD_MESSAGE_REFERENCE[contains(translate(TM_FLD_SECTION,$lowercase,$uppercase),'FRONT')]/TM_FLD_REF_MESG" /></xsl:call-template> 
          <!-- Ivor 15122012 Targeted Bill Message M2R2- Front End -->
        </BILL_MSG_SEGMENT>
        <EMAIL_TO>
          <xsl:if test="/invoice/PIN_FLD_ACCTINFO/PIN_FLD_DELIVERY_DESCR != ''">
            <xsl:value-of select="/invoice/PIN_FLD_ACCTINFO/PIN_FLD_DELIVERY_DESCR"/>
          </xsl:if>
        </EMAIL_TO>
        <EMAIL_CC>
          <xsl:if test="/invoice/PIN_FLD_ACCTINFO/PIN_FLD_EMAIL_ADDR != ''">
            <xsl:value-of select="/invoice/PIN_FLD_ACCTINFO/PIN_FLD_EMAIL_ADDR"/>
          </xsl:if>
        </EMAIL_CC>
      </BILL>
      <!--   Summary Charges   -->
      <SUMMARY>
        <CURRENT_CHARGES>
          <TOTAL_RECURRING>
		   <xsl:choose>
                <xsl:when test="contains(format-number(sum(/invoice/PIN_FLD_AR_ITEMS/PIN_FLD_EVENTS[contains(PIN_FLD_ITEM_OBJ,'cycle')][not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT),'###,###,###,##0.00'),'-0.00')">0.00</xsl:when>
                <xsl:otherwise>
                        <xsl:value-of select="format-number(sum(/invoice/PIN_FLD_AR_ITEMS/PIN_FLD_EVENTS[contains(PIN_FLD_ITEM_OBJ,'cycle')][not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT),'###,###,###,##0.00')" />
                </xsl:otherwise>
			</xsl:choose>
          </TOTAL_RECURRING>
          <TOTAL_OTC>
            <xsl:value-of select="format-number(sum(/invoice/PIN_FLD_AR_ITEMS/PIN_FLD_EVENTS[not(contains(PIN_FLD_ITEM_OBJ,'cycle'))][not(contains(PIN_FLD_ITEM_OBJ,'usage'))][not(contains(PIN_FLD_ITEM_OBJ,'iptv'))][not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))][not(contains(PIN_FLD_ITEM_OBJ,'burstable'))]/PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
          </TOTAL_OTC>
          <TOTAL_USG>
            <!-- amer 20130820 : include the burstable usage amount -->
            <xsl:value-of select="$totalAllUSG"/>
          </TOTAL_USG>
          <TOTAL_TAX>
            <xsl:value-of select="format-number($totalTax,'###,###,###,##0.00')"/>
          </TOTAL_TAX>
          <TOTAL_CURR_CR_ADJ>0.00</TOTAL_CURR_CR_ADJ>
          <TOTAL_CURR_DR_ADJ>
            <xsl:value-of select="$currAdj"/>
          </TOTAL_CURR_DR_ADJ>
          <TOTAL_CURRENT>
            <xsl:value-of select="format-number(invoice/PIN_FLD_BILLINFO/PIN_FLD_CURRENT_TOTAL, '###,###,###,##0.00')"/>
          </TOTAL_CURRENT>
          <TOTAL_CURRENT_DUE>
            <xsl:value-of select="format-number(invoice/PIN_FLD_BILLINFO/PIN_FLD_CURRENT_TOTAL +  $totalOutstanding,'###,###,###,##0.00')"/>
          </TOTAL_CURRENT_DUE>
          <ROUNDING_ADJ>
            <xsl:value-of select="format-number($totalRoundingAdj,'###,###,###,##0.00')"/>
          </ROUNDING_ADJ>
          <TOTAL_DUE>
            <xsl:value-of select="format-number(invoice/PIN_FLD_BILLINFO/PIN_FLD_CURRENT_TOTAL + $totalOutstanding + $totalRoundingAdj,'###,###,###,##0.00')"/>
          </TOTAL_DUE>
        </CURRENT_CHARGES>
        <PREVIOUS_CHARGES>
          <TOTAL_PREV>
            <xsl:value-of select="format-number(invoice/PIN_FLD_BILLINFO/PIN_FLD_PREVIOUS_TOTAL - $totalPayments - $prevAdj,'###,###,###,##0.00')"/>
          </TOTAL_PREV>
          <TOTAL_PREV_CR_ADJ>
            <xsl:value-of select="format-number($prevAdj_CR,'###,###,###,##0.00')"/>
          </TOTAL_PREV_CR_ADJ>
          <TOTAL_PREV_DR_ADJ>
            <xsl:value-of select="format-number($prevAdj_DB,'###,###,###,##0.00')"/>
          </TOTAL_PREV_DR_ADJ>
          <TOTAL_PREV_PYMT>
            <xsl:value-of select="format-number($totalPayments,'###,###,###,##0.00')"/>
          </TOTAL_PREV_PYMT>
          <TOTAL_OUTSTANDING>
            <xsl:value-of select="format-number($totalOutstanding,'###,###,###,##0.00')"/>
          </TOTAL_OUTSTANDING>
        </PREVIOUS_CHARGES>
        <!-- PREV_BAL><xsl:if test="invoice/PIN_FLD_BILLINFO/PIN_FLD_PREVIOUS_TOTAL  != 0"><xsl:value-of select="format-number(invoice/PIN_FLD_BILLINFO/PIN_FLD_PREVIOUS_TOTAL - $totalPayments - $totalCreditAdj + $totalDebitAdj,'###,###,###,##0.00')"/></xsl:if><xsl:if test="invoice/PIN_FLD_BILLINFO/PIN_FLD_PREVIOUS_TOTAL  = 0"><xsl:value-of select="invoice/PIN_FLD_BILLINFO/PIN_FLD_PREVIOUS_TOTAL"/></xsl:if></PREV_BAL -->
        <!--   <TOTAL_AMOUNT><xsl:value-of select="format-number(invoice/PIN_FLD_BILLINFO/PIN_FLD_CURRENT_TOTAL + invoice/PIN_FLD_BILLINFO/PIN_FLD_PREVIOUS_TOTAL,'###,###,###,##0.00')"/></TOTAL_AMOUNT>   -->
      </SUMMARY>
      <!-- amer - LARS CR562 -->
      <xsl:if test="/invoice/PIN_FLD_ACCTINFO/TM_FLD_REWARD_MEMBER_NO">
        <xsl:choose>
          <xsl:when test="/invoice/PIN_FLD_ACCTINFO/TM_FLD_REWARD_MEMBER_NO = ''">
            <TM_REWARD_SEGMENT/>
          </xsl:when>
          <xsl:otherwise>
            <TM_REWARD_SEGMENT>
              <TM_REWARD_MEMBER_NO>
                <xsl:value-of select="invoice/PIN_FLD_ACCTINFO/TM_FLD_REWARD_MEMBER_NO"/>
              </TM_REWARD_MEMBER_NO>
              <TM_REWARD_POINT>
                <xsl:value-of select="invoice/PIN_FLD_ACCTINFO/TM_FLD_REWARD_POINT"/>
              </TM_REWARD_POINT>
              <TM_REWARD_TODATE>
                <xsl:value-of select="invoice/PIN_FLD_ACCTINFO/TM_FLD_REWARD_POINT_LASTDATE"/>
              </TM_REWARD_TODATE>
            </TM_REWARD_SEGMENT>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:if>
      <DETAIL>
        <!--  Payment Details  -->
        <xsl:if test="invoice/PIN_FLD_OTHER_ITEMS[contains(PIN_FLD_NAME,'Payment')]/PIN_FLD_ITEM_TOTAL != 0">
          <PAYMENTS>
            <xsl:for-each select="invoice/PIN_FLD_OTHER_ITEMS[contains(PIN_FLD_NAME,'Payment')]">
              <PYMT_DETAILS>
                <xsl:choose>
                  <xsl:when test="not(contains(.,'Reversal'))">
                    <!--  Normal Payment Reversal  -->
                    <PYMT_DATE>
                      <xsl:call-template name="format-unix-date">
                        <xsl:with-param name="unix" select="PIN_FLD_EVENTS/PIN_FLD_EFFECTIVE_T"/>
                      </xsl:call-template>
                    </PYMT_DATE>
                    <PYMT_DESC>
                      <xsl:choose>
                        <xsl:when test="translate(/invoice/PIN_FLD_ACCTINFO/TM_FLD_BILL_LANGUAGE,$lowercase,$uppercase) = 'MAL'">Bayaran</xsl:when>
                        <xsl:otherwise>
                          <xsl:value-of select="PIN_FLD_NAME "/>
                        </xsl:otherwise>
                      </xsl:choose> - <xsl:value-of select="PIN_FLD_DESCR "/>
                    </PYMT_DESC>
                  </xsl:when>
                  <xsl:otherwise>
                    <!--  Payment Reversal  -->
                    <PYMT_DATE>
                      <xsl:call-template name="format-unix-date">
                        <xsl:with-param name="unix" select="PIN_FLD_EFFECTIVE_T"/>
                      </xsl:call-template>
                    </PYMT_DATE>
                    <PYMT_DESC>
                      <xsl:choose>
                        <xsl:when test="translate(/invoice/PIN_FLD_ACCTINFO/TM_FLD_BILL_LANGUAGE,$lowercase,$uppercase) = 'MAL'">
                          <xsl:choose>
                            <xsl:when test="PIN_FLD_DESCR != ''">Bayaran Pelarasan - <xsl:value-of select="PIN_FLD_DESCR"/>
                            </xsl:when>
                            <xsl:otherwise>Bayaran Pelarasan</xsl:otherwise>
                          </xsl:choose>
                        </xsl:when>
                        <xsl:otherwise>
                          <xsl:choose>
                            <xsl:when test="PIN_FLD_DESCR != ''">
                              <xsl:value-of select="PIN_FLD_NAME "/> - <xsl:value-of select="PIN_FLD_DESCR"/>
                            </xsl:when>
                            <xsl:otherwise>
                              <xsl:value-of select="PIN_FLD_NAME "/>
                            </xsl:otherwise>
                          </xsl:choose>
                        </xsl:otherwise>
                      </xsl:choose>
                    </PYMT_DESC>
                  </xsl:otherwise>
                </xsl:choose>
                <PYMT_AMT>
                  <xsl:value-of select="format-number(PIN_FLD_ITEM_TOTAL,'###,###,###,##0.00')"/>
                </PYMT_AMT>
              </PYMT_DETAILS>
            </xsl:for-each>
            <TOTAL_PYMT>
              <xsl:value-of select="format-number($totalPayments,'###,###,###,##0.00')"/>
            </TOTAL_PYMT>
          </PAYMENTS>
        </xsl:if>
        <!--  Adjusment Details  -->
        <xsl:if test="invoice/PIN_FLD_OTHER_ITEMS[contains(PIN_FLD_NAME,'Adjustment')]/PIN_FLD_EVENTS[not(contains(PIN_FLD_DESCR,'5 Sen Government Rounding'))]/PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT != 0">
          <ADJUSTMENTS>
            <xsl:for-each select="invoice/PIN_FLD_OTHER_ITEMS[contains(PIN_FLD_NAME,'Adjustment')][not(contains(PIN_FLD_EVENTS/PIN_FLD_DESCR,'5 Sen Government Rounding'))]">
              <ADJ_DETAILS>
                <ADJ_DATE>
                  <xsl:if test="PIN_FLD_EVENTS/PIN_FLD_END_T != ''">
                    <xsl:call-template name="format-unix-date">
                      <xsl:with-param name="unix" select="PIN_FLD_EVENTS/PIN_FLD_END_T"/>
                    </xsl:call-template>
                  </xsl:if>
                </ADJ_DATE>
                <ADJ_DESC>
                <xsl:choose>
	                <xsl:when test="contains(PIN_FLD_EVENTS/PIN_FLD_DESCR,'|')">
	                  <xsl:value-of select="substring-after(PIN_FLD_EVENTS/PIN_FLD_DESCR,'|')"/>
	                </xsl:when>
	                <xsl:otherwise>
                  <xsl:value-of select="PIN_FLD_EVENTS/PIN_FLD_DESCR"/>
	                </xsl:otherwise>
                  </xsl:choose>
                </ADJ_DESC>
                <ADJ_TYPE>
                  <xsl:choose>
                    <xsl:when test="PIN_FLD_EVENTS/PIN_FLD_TOTAL[contains(PIN_FLD_AMOUNT,'-')]">CREDIT</xsl:when>
                    <xsl:otherwise>DEBIT</xsl:otherwise>
                  </xsl:choose>
                </ADJ_TYPE>
                <xsl:variable name="adjGross">
                  <xsl:choose>
                    <xsl:when test="PIN_FLD_ITEM_TOTAL != '' and PIN_FLD_EVENTS/PIN_FLD_BAL_IMPACTS[PIN_FLD_GL_ID = '20001083' or PIN_FLD_GL_ID = '20001084' or PIN_FLD_GL_ID = '20001085' or PIN_FLD_GL_ID = '21001082' or PIN_FLD_GL_ID = '21001083' or PIN_FLD_GL_ID = '21001084' or PIN_FLD_GL_ID = '20001003' or PIN_FLD_GL_ID = '21001003' ]">
                      <xsl:value-of select="format-number('0','##0.00')"/>
                    </xsl:when>
                    <xsl:when test="PIN_FLD_ITEM_TOTAL != ''">
                      <xsl:value-of select="format-number(sum(PIN_FLD_EVENTS/PIN_FLD_BAL_IMPACTS[PIN_FLD_IMPACT_TYPE != 4]/PIN_FLD_AMOUNT),'##0.00')"/>
                    </xsl:when>
                    <xsl:otherwise>0.00</xsl:otherwise>
                  </xsl:choose>
                </xsl:variable>
                <xsl:variable name="adjTax">
                  <xsl:choose>
                    <xsl:when test="PIN_FLD_EVENTS/PIN_FLD_TAXES/PIN_FLD_TAX != ''">
                      <xsl:value-of select="format-number(round(100*sum(PIN_FLD_EVENTS/PIN_FLD_TAXES/PIN_FLD_TAX)) div 100,'##0.00')"/>
                      <!-- format-number(round(100*PIN_FLD_EVENTS/PIN_FLD_TAXES/PIN_FLD_TAX) div 100 -->
                    </xsl:when>
                    <xsl:otherwise>0.00</xsl:otherwise>
                  </xsl:choose>
                </xsl:variable>
                <ADJ_AMT>
                  <xsl:value-of select="format-number($adjTax + $adjGross,'###,###,###,##0.00')"/>
                </ADJ_AMT>
                <ADJ_GROSS>
                  <xsl:value-of select="format-number($adjGross,'###,###,###,##0.00')"/>
                </ADJ_GROSS>
                <ADJ_TAX>
                  <xsl:value-of select="format-number($adjTax,'###,###,###,##0.00')"/>
                </ADJ_TAX>
                <ADJ_INV_NO>
                  <xsl:value-of select="PIN_FLD_BILL_NO"/>
                </ADJ_INV_NO>
				
				<!-- William 20121024 - Service Level Adjustment - Start -->
				<ADJ_SERVICE_NO>
				<xsl:value-of select="PIN_FLD_LOGIN"/>
				</ADJ_SERVICE_NO>
				<!-- William 20121024 - Service Level Adjustment - End -->
              </ADJ_DETAILS>
            </xsl:for-each>
            <!-- Added by David Khaw 19 February 2013 [PIN_FLD_IMPACT_TYPE != 4] -->
            <xsl:variable name="totalCRAdjGrossWGLID">
              <!-- Minus off the ADJ Tax GLID (8GLID) -->
              <xsl:value-of select="sum(invoice/PIN_FLD_OTHER_ITEMS[contains(PIN_FLD_NAME,'Adjustment')][not(contains(PIN_FLD_EVENTS/PIN_FLD_DESCR,'5 Sen Government Rounding'))]/PIN_FLD_EVENTS[not (contains(PIN_FLD_EVENT_OBJ,'tax_event'))]/PIN_FLD_BAL_IMPACTS[contains(PIN_FLD_AMOUNT,'-')][PIN_FLD_GL_ID = '20001083' or PIN_FLD_GL_ID = '20001084' or PIN_FLD_GL_ID = '20001085' or PIN_FLD_GL_ID = '21001082' or PIN_FLD_GL_ID = '21001083' or PIN_FLD_GL_ID = '21001084' or PIN_FLD_GL_ID = '20001003' or PIN_FLD_GL_ID = '21001003' or PIN_FLD_GL_ID = '40000000'][PIN_FLD_IMPACT_TYPE != 4]/PIN_FLD_AMOUNT) "/>
            </xsl:variable>
            <xsl:variable name="totalCRAdjGross">
              <xsl:value-of select="sum(invoice/PIN_FLD_OTHER_ITEMS[contains(PIN_FLD_NAME,'Adjustment')][not(contains(PIN_FLD_EVENTS/PIN_FLD_DESCR,'5 Sen Government Rounding'))]/PIN_FLD_EVENTS[not (contains(PIN_FLD_EVENT_OBJ,'tax_event'))]/PIN_FLD_BAL_IMPACTS[contains(PIN_FLD_AMOUNT,'-')][PIN_FLD_IMPACT_TYPE != 4]/PIN_FLD_AMOUNT) "/>
            </xsl:variable>
            <xsl:variable name="totalCRAdjTax">
              <xsl:value-of select="format-number(round(100*sum(invoice/PIN_FLD_OTHER_ITEMS[contains(PIN_FLD_NAME,'Adjustment')][not(contains(PIN_FLD_EVENTS/PIN_FLD_DESCR,'5 Sen Government Rounding'))]/PIN_FLD_EVENTS/PIN_FLD_TAXES[contains(PIN_FLD_TAX,'-')]/PIN_FLD_TAX)) div 100,'##0.00')"/>
            </xsl:variable>
            <!-- Added by David Khaw 19 February 2013 [PIN_FLD_IMPACT_TYPE != 4] -->
            <xsl:variable name="totalDBAdjGrossWGLID">
              <!-- Minus off the ADJ Tax GLID (8GLID) -->
              <xsl:value-of select="sum(invoice/PIN_FLD_OTHER_ITEMS[contains(PIN_FLD_NAME,'Adjustment')][not(contains(PIN_FLD_EVENTS/PIN_FLD_DESCR,'5 Sen Government Rounding'))]/PIN_FLD_EVENTS[not (contains(PIN_FLD_EVENT_OBJ,'tax_event'))]/PIN_FLD_BAL_IMPACTS[not (contains(PIN_FLD_AMOUNT,'-'))][PIN_FLD_GL_ID = '20001083' or PIN_FLD_GL_ID = '20001084' or PIN_FLD_GL_ID = '20001085' or PIN_FLD_GL_ID = '21001082' or PIN_FLD_GL_ID = '21001083' or PIN_FLD_GL_ID = '21001084' or PIN_FLD_GL_ID = '20001003' or PIN_FLD_GL_ID = '21001003' or PIN_FLD_GL_ID = '40000000'][PIN_FLD_IMPACT_TYPE != 4]/PIN_FLD_AMOUNT) "/>
            </xsl:variable>
            <xsl:variable name="totalDBAdjGross">
              <xsl:value-of select="sum(invoice/PIN_FLD_OTHER_ITEMS[contains(PIN_FLD_NAME,'Adjustment')][not(contains(PIN_FLD_EVENTS/PIN_FLD_DESCR,'5 Sen Government Rounding'))]/PIN_FLD_EVENTS[not (contains(PIN_FLD_EVENT_OBJ,'tax_event'))]/PIN_FLD_BAL_IMPACTS[not (contains(PIN_FLD_AMOUNT,'-'))][PIN_FLD_IMPACT_TYPE != 4]/PIN_FLD_AMOUNT) "/>
            </xsl:variable>
            <xsl:variable name="totalDBAdjTax">
              <xsl:value-of select="format-number(round(100*sum(invoice/PIN_FLD_OTHER_ITEMS[contains(PIN_FLD_NAME,'Adjustment')][not(contains(PIN_FLD_EVENTS/PIN_FLD_DESCR,'5 Sen Government Rounding'))]/PIN_FLD_EVENTS/PIN_FLD_TAXES[not (contains(PIN_FLD_TAX,'-'))]/PIN_FLD_TAX)) div 100,'##0.00')"/>
            </xsl:variable>
            <TOTAL_CR_ADJ>
              <xsl:value-of select="format-number($totalCRAdjGross - $totalCRAdjGrossWGLID + $totalCRAdjTax,'###,###,###,##0.00')"/>
            </TOTAL_CR_ADJ>
            <TOTAL_CR_ADJ_GROSS>
              <xsl:value-of select="format-number($totalCRAdjGross - $totalCRAdjGrossWGLID,'###,###,###,##0.00')"/>
            </TOTAL_CR_ADJ_GROSS>
            <TOTAL_CR_ADJ_TAX>
              <xsl:value-of select="format-number($totalCRAdjTax,'###,###,###,##0.00')"/>
            </TOTAL_CR_ADJ_TAX>
            <TOTAL_DR_ADJ>
              <xsl:value-of select="format-number($totalDBAdjGross - $totalDBAdjGrossWGLID + $totalDBAdjTax,'###,###,###,##0.00')"/>
            </TOTAL_DR_ADJ>
            <TOTAL_DR_ADJ_GROSS>
              <xsl:value-of select="format-number($totalDBAdjGross - $totalDBAdjGrossWGLID,'###,###,###,##0.00')"/>
            </TOTAL_DR_ADJ_GROSS>
            <TOTAL_DR_ADJ_TAX>
              <xsl:value-of select="format-number($totalDBAdjTax,'###,###,###,##0.00')"/>
            </TOTAL_DR_ADJ_TAX>
          </ADJUSTMENTS>
        </xsl:if>
        <!--  Usage Summary  -->
        <xsl:if test="/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_ITEM_OBJ,'burstable')]/PIN_FLD_EVENTS[contains(PIN_FLD_DESCR, 'BURST')] or /invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_ITEM_OBJ,'usage') or contains(PIN_FLD_ITEM_OBJ,'iptv')]/PIN_FLD_ITEM_TOTAL != 0 or /invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_ITEM_OBJ,'usage')]/PIN_FLD_EVENTS/PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID=1000107]/PIN_FLD_QUANTITY != 0">
          <USG_SUMMARY>
            <xsl:call-template name="event-template-Usage">
              <xsl:with-param name="item-list" select="/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_ITEM_OBJ,'usage') or contains(PIN_FLD_ITEM_OBJ,'iptv') or contains(PIN_FLD_ITEM_OBJ,'burstable')]"/>
            </xsl:call-template>
            <!--  <xsl:call-template name="event-template-Usage"><xsl:with-param name="item-list" select="/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_ITEM_OBJ,'iptv')]" /></xsl:call-template>  -->
          </USG_SUMMARY>
        </xsl:if>
        <!--  Tax Summary  -->
        <xsl:if test="/invoice/PIN_FLD_AR_ITEMS[PIN_FLD_ITEM_TOTAL != 0]/PIN_FLD_EVENTS[contains(PIN_FLD_ITEM_OBJ,'cycle_tax')]">
          <TAX_SUMMARY>
            <TOTAL_AMT_TAXABLE>
              <xsl:value-of select="format-number(sum(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_ITEM_OBJ,'cycle_tax')]/PIN_FLD_EVENTS/PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id][PIN_FLD_TAX_CODE != 'NORM'][PIN_FLD_TAX_CODE != '']/PIN_FLD_QUANTITY),'###,###,###,##0.00')"/>
            </TOTAL_AMT_TAXABLE>
            <TOTAL_AMT_TAX>
              <xsl:value-of select="format-number($totalTax,'###,###,###,##0.00')"/>
            </TOTAL_AMT_TAX>
          </TAX_SUMMARY>
        </xsl:if>
        <!-- RC Summary -->
        <xsl:if test="/invoice/PIN_FLD_AR_ITEMS[not(contains(PIN_FLD_SERVICE_OBJ,'0.0.0.0  0 0'))][contains(PIN_FLD_ITEM_OBJ,'cycle')][not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]">
          <RC_SUMMARY>
            <xsl:variable name="currentAccountObj" select="substring-before(substring-after(substring-after(/invoice/PIN_FLD_ACCTINFO/PIN_FLD_ACCOUNT_OBJ,' '),' '),' ')"/>
            <!--   Display HSI Service   -->
            <xsl:for-each select="/invoice/PIN_FLD_AR_ITEMS[generate-id(.) = generate-id(key('SERVICE-by-SERVICE_OBJ', substring-before(substring-after(substring-after(PIN_FLD_SERVICE_OBJ,' '),' '),' ')))][contains(PIN_FLD_SERVICE_OBJ,'tm_hsi')]">
              <xsl:sort select="(*|*/*)[name()='PIN_FLD_START_TIME']" order="ascending"/>
              <xsl:call-template name="event-template-Subscription-Charges-Summary">
                <xsl:with-param name="event-list" select="."/>
              </xsl:call-template>
            </xsl:for-each>
            <!--   Display IPTV Service   -->
            <xsl:for-each select="/invoice/PIN_FLD_AR_ITEMS[generate-id(.) = generate-id(key('SERVICE-by-SERVICE_OBJ', substring-before(substring-after(substring-after(PIN_FLD_SERVICE_OBJ,' '),' '),' ')))][contains(PIN_FLD_SERVICE_OBJ,'tm_iptv')]">
              <xsl:sort select="(*|*/*)[name()='PIN_FLD_START_TIME']" order="ascending"/>
              <xsl:call-template name="event-template-Subscription-Charges-Summary">
                <xsl:with-param name="event-list" select="."/>
              </xsl:call-template>
            </xsl:for-each>
            <!--   Display VOICE Service   -->
            <xsl:for-each select="/invoice/PIN_FLD_AR_ITEMS[generate-id(.) = generate-id(key('SERVICE-by-SERVICE_OBJ', substring-before(substring-after(substring-after(PIN_FLD_SERVICE_OBJ,' '),' '),' ')))][contains(PIN_FLD_SERVICE_OBJ,'telephony')]">
              <xsl:sort select="(*|*/*)[name()='PIN_FLD_START_TIME']" order="ascending"/>
              <xsl:call-template name="event-template-Subscription-Charges-Summary">
                <xsl:with-param name="event-list" select="."/>
              </xsl:call-template>
            </xsl:for-each>
            
            <!-- Display Network and Leg Grouping -->
            <xsl:for-each select="/invoice/TM_FLD_CUST_SRV_NW_PROFILE/PIN_FLD_SERVICES[generate-id(.) = generate-id(key('NETWORK-by-SERVICE_OBJ', substring-before(substring-after(substring-after(PIN_FLD_SERVICE_OBJ,' '),' '),' ')))]">
            <xsl:sort select="(*|*/*)[name()='PIN_FLD_ORDER_COUNT']" order="ascending"/>
            <xsl:sort select="(*|*/*)[name()='PIN_FLD_LOGIN']" order="ascending"/>
	            <xsl:variable name="networkService" select="./PIN_FLD_SERVICE_OBJ"/> 
	            <xsl:call-template name="event-template-Subscription-Charges-Summary-Network">
	            <xsl:with-param name="serv-poid" select="$networkService"/>
	            </xsl:call-template>
            </xsl:for-each>
            <!--   Display Other Services   -->
            <xsl:for-each select="/invoice/PIN_FLD_AR_ITEMS[generate-id(.) = generate-id(key('SERVICE-by-SERVICE_OBJ', substring-before(substring-after(substring-after(PIN_FLD_SERVICE_OBJ,' '),' '),' ')))][not(contains(PIN_FLD_SERVICE_OBJ,'tm_hsi')) and not(contains(PIN_FLD_SERVICE_OBJ,'tm_iptv')) and not(contains(PIN_FLD_SERVICE_OBJ,'telephony')) and not(contains(PIN_FLD_SERVICE_OBJ,'0.0.0.0  0 0'))]">
              <xsl:sort select="(*|*/*)[name()='PIN_FLD_ORDER_COUNT']" order="ascending"/>
              <!-- <xsl:sort select="(*|*/*)[name()='PIN_FLD_START_TIME']" order="ascending"/> --> <!-- For BE774 sorting -->
              <xsl:sort select="(*|*/*)[name()='PIN_FLD_LOGIN']" order="ascending"/>
              <xsl:variable name="currentServ" select="substring-before(substring-after(substring-after(./PIN_FLD_SERVICE_OBJ,' '),' '),' ')"/>
              <xsl:variable name="currentActServ" select="./PIN_FLD_SERVICE_OBJ"/>
              <!-- Leg service : Handled Above, do nothing (2013-05-14) -->
              <xsl:if test="/invoice/TM_FLD_CUST_SRV_NW_PROFILE/PIN_FLD_SERVICES/PIN_FLD_SERVICES[contains(PIN_FLD_SERVICE_OBJ,$currentServ)]">
                <!-- DO NOTHING -->
              </xsl:if>
              
  
              	<xsl:variable name="legNetw" select="/invoice/TM_FLD_CUST_SRV_ATT_PROFILE/PIN_FLD_SERVICES[contains(PIN_FLD_SERVICE_OBJ,$currentServ)]/PIN_FLD_NETWORK_ELEMENT_INFO/PIN_FLD_NETWORK_ID"/>
              	  <xsl:variable name="legNoProfile">
				    <xsl:choose>
				      <xsl:when test="/invoice/TM_FLD_CUST_SRV_NW_PROFILE/PIN_FLD_SERVICES/PIN_FLD_NETWORK_ELEMENT_INFO/PIN_FLD_NETWORK_ID = $legNetw">
				        <xsl:value-of select="$legNetw"/>
				      </xsl:when>
				      <xsl:otherwise>LegNoProfile</xsl:otherwise>
				    </xsl:choose>
				  </xsl:variable>
				
				<!-- Trunk service : if no trunk at profile, then show in the summary (2014-05-15) -->
				<xsl:variable name="trNetw" select="/invoice/TM_FLD_CUST_SRV_ATT_PROFILE/PIN_FLD_SERVICES[contains(PIN_FLD_SERVICE_OBJ,$currentServ)]/PIN_FLD_NETWORK_ELEMENT_INFO/PIN_FLD_NETWORK_ID"/>
              	  <xsl:variable name="trunkNoProfile">
				    <xsl:choose>
				      <xsl:when test="/invoice/TM_FLD_CUST_SRV_NW_PROFILE/PIN_FLD_SERVICES/PIN_FLD_NETWORK_ELEMENT_INFO/PIN_FLD_NETWORK_ID = $trNetw">
				        <xsl:value-of select="$trNetw"/>
				      </xsl:when>
				      <xsl:otherwise>trunkNoProfile</xsl:otherwise>
				    </xsl:choose>
				  </xsl:variable>
				
              <!-- If Network Service, Do Nothing (2013-05-14) -->
              <xsl:if test="/invoice/TM_FLD_CUST_SRV_NW_PROFILE/PIN_FLD_SERVICES[contains(PIN_FLD_SERVICE_OBJ,$currentServ)]">
              	<!-- DO NOTHING -->
              </xsl:if>
              <!-- If not network and not leg -->
              <xsl:if test="not(/invoice/TM_FLD_CUST_SRV_NW_PROFILE/PIN_FLD_SERVICES/PIN_FLD_SERVICE_OBJ = $currentActServ)">
                <xsl:if test="not(/invoice/TM_FLD_CUST_SRV_NW_PROFILE/PIN_FLD_SERVICES/PIN_FLD_SERVICES/PIN_FLD_SERVICE_OBJ = $currentActServ)">
                   <xsl:if test="not(contains($currentActServ,'leg')) or $legNoProfile = 'LegNoProfile'">
				   <!-- Trunk service : if no trunk at profile, then show in the summary (2014-05-15) -->
					<xsl:if test="not(contains($currentActServ,'trunk')) or $trunkNoProfile = 'trunkNoProfile' ">
						<xsl:call-template name="event-template-Subscription-Charges-Summary">
							<xsl:with-param name="event-list" select="."/>
						</xsl:call-template>
					</xsl:if>
	               </xsl:if>
                </xsl:if>
              </xsl:if>
            </xsl:for-each>
            <xsl:choose>
              <xsl:when test="contains(/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP,'WHOLESALE') or contains(/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP,'GLOBAL')">
                <AMT_GROSS_TOTAL/>
                <AMT_DISCOUNT_TOTAL/>
              </xsl:when>
              <xsl:otherwise>
                <AMT_GROSS_TOTAL>
                  <xsl:value-of select="format-number(sum(/invoice/PIN_FLD_AR_ITEMS[not(contains(PIN_FLD_SERVICE_OBJ,'0.0.0.0  0 0'))][contains(PIN_FLD_ACCOUNT_OBJ,$currentAccountObj)][contains(PIN_FLD_ITEM_OBJ,'cycle')][not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_EVENTS/PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id][PIN_FLD_IMPACT_TYPE != 128 and PIN_FLD_IMPACT_TYPE != 512]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
                </AMT_GROSS_TOTAL>
                <AMT_DISCOUNT_TOTAL>
                  <xsl:value-of select="format-number(sum(/invoice/PIN_FLD_AR_ITEMS[not(contains(PIN_FLD_SERVICE_OBJ,'0.0.0.0  0 0'))][contains(PIN_FLD_ACCOUNT_OBJ,$currentAccountObj)][contains(PIN_FLD_ITEM_OBJ,'cycle')][not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_EVENTS/PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id][PIN_FLD_IMPACT_TYPE = 128]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
                </AMT_DISCOUNT_TOTAL>
              </xsl:otherwise>
            </xsl:choose>
            <AMT_NETT_TOTAL>
			<xsl:choose>
			<xsl:when test="contains(format-number(sum(/invoice/PIN_FLD_AR_ITEMS[not(contains(PIN_FLD_SERVICE_OBJ,'0.0.0.0  0 0'))][contains(PIN_FLD_ACCOUNT_OBJ,$currentAccountObj)][contains(PIN_FLD_ITEM_OBJ,'cycle')][not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_EVENTS/PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT),'###,###,###,##0.00'),'-0.00')">0.00</xsl:when>
			<xsl:otherwise>
					<xsl:value-of select="format-number(sum(/invoice/PIN_FLD_AR_ITEMS[not(contains(PIN_FLD_SERVICE_OBJ,'0.0.0.0  0 0'))][contains(PIN_FLD_ACCOUNT_OBJ,$currentAccountObj)][contains(PIN_FLD_ITEM_OBJ,'cycle')][not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_EVENTS/PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT),'###,###,###,##0.00')" />
			</xsl:otherwise>
			</xsl:choose>      
            </AMT_NETT_TOTAL>
          </RC_SUMMARY>
        </xsl:if>
        <!--  Account & Service Charges  -->
        <xsl:call-template name="event-template-Account">
          <xsl:with-param name="item-list" select="invoice"/>
        </xsl:call-template>
      </DETAIL>
      <!--  Statement of Account  -->
      <xsl:if test="invoice/PIN_FLD_BILLS">
        <!-- check first if there is any positive bills - Amer -->
        <xsl:variable name="maxOutstandingBill">
          <xsl:for-each select="invoice/PIN_FLD_BILLS/PIN_FLD_CURRENT_TOTAL">
            <xsl:sort data-type="number" order="descending"/>
            <xsl:if test="position()=1">
              <xsl:value-of select="."/>
            </xsl:if>
          </xsl:for-each>
        </xsl:variable>
        <!-- only display SOA if the max outstanding is positive  -->
        <xsl:if test="$maxOutstandingBill &gt; 0">
          <xsl:if test="(invoice/PIN_FLD_BILLINFO/PIN_FLD_PREVIOUS_TOTAL - $totalPayments - $prevAdj) &gt; 0">
            <xsl:if test="invoice/PIN_FLD_BILLS/PIN_FLD_DUE &gt; 0">
              <ACC_STATEMENT>
                <!--  <BILLS>
<BILL_NO>Open</BILL_NO>
<BILL_DATE><xsl:call-template name="format-unix-date"><xsl:with-param name="unix" select="/invoice/PIN_FLD_BILLINFO/PIN_FLD_END_T" /></xsl:call-template></BILL_DATE>
<BILL_TOTAL><xsl:value-of select="format-number(0.00,'###,###,###,##0.00')"/></BILL_TOTAL>
<PYMT_RCVD><xsl:value-of select="format-number(/invoice/PIN_FLD_BILLINFO/PIN_FLD_AMOUNT_ORIGINAL_PAYMENT,'###,###,###,##0.00')"/></PYMT_RCVD>
<ADJ_TOTAL><xsl:value-of select="format-number(/invoice/PIN_FLD_BILLINFO/PIN_FLD_UNAPPLIED_AMOUNT,'###,###,###,##0.00')"/></ADJ_TOTAL>
<TOTAL><xsl:value-of select="format-number(/invoice/PIN_FLD_BILLINFO/PIN_FLD_AMOUNT_ORIGINAL_PAYMENT + /invoice/PIN_FLD_BILLINFO/PIN_FLD_UNAPPLIED_AMOUNT,'###,###,###,##0.00')"/></TOTAL>
</BILLS>  -->
                <xsl:for-each select="invoice/PIN_FLD_BILLS">
                  <xsl:variable name="totalAdjustment">
                    <xsl:value-of select="((PIN_FLD_DUE - (PIN_FLD_RECVD - PIN_FLD_TRANSFERED)) - PIN_FLD_CURRENT_TOTAL)"/>
                  </xsl:variable>
                  <BILLS>
                    <BILL_NO>
                      <xsl:value-of select="PIN_FLD_BILL_NO"/>
                    </BILL_NO>
                    <BILL_DATE>
                      <xsl:call-template name="format-unix-date">
                        <xsl:with-param name="unix" select="PIN_FLD_END_T"/>
                      </xsl:call-template>
                    </BILL_DATE>
                    <BILL_TOTAL>
                      <xsl:value-of select="format-number(PIN_FLD_CURRENT_TOTAL,'###,###,###,##0.00')"/>
                    </BILL_TOTAL>
                    <PYMT_RCVD>
                      <xsl:value-of select="format-number(PIN_FLD_RECVD - PIN_FLD_TRANSFERED,'###,###,###,##0.00')"/>
                    </PYMT_RCVD>
                    <!--     <ADJ_TOTAL><xsl:value-of select="format-number(PIN_FLD_ADJUSTED,'###,###,###,##0.00')"/></ADJ_TOTAL>  -->
                    <ADJ_TOTAL>
                      <xsl:choose>
                        <xsl:when test="contains(format-number($totalAdjustment,'###,###,###,##0.00'),'-0.00')">0.00</xsl:when>
                        <xsl:otherwise>
                          <xsl:value-of select="format-number($totalAdjustment,'###,###,###,##0.00')"/>
                        </xsl:otherwise>
                      </xsl:choose>
                    </ADJ_TOTAL>
                    <TOTAL>
                      <xsl:choose>
                        <xsl:when test="contains(format-number(PIN_FLD_DUE,'###,###,###,##0.00'),'-0.00')">0.00</xsl:when>
                        <xsl:otherwise>
                          <xsl:value-of select="format-number(PIN_FLD_DUE,'###,###,###,##0.00')"/>
                        </xsl:otherwise>
                      </xsl:choose>
                    </TOTAL>
                  </BILLS>
                </xsl:for-each>
              </ACC_STATEMENT>
            </xsl:if>
          </xsl:if>
        </xsl:if>
      </xsl:if>
      <!-- end of only display SOA if there is positive bill amer  -->
	  	<!-- Ivor 15122012 Targeted Bill Message M2R2 - Back Start -->
	<xsl:call-template name="bill-Message-back"><xsl:with-param name="event-list" select="/invoice/PIN_FLD_MESSAGE_REFERENCE[contains(translate(TM_FLD_SECTION,$lowercase,$uppercase),'BACK')]/TM_FLD_REF_MESG" /> </xsl:call-template>
	<!-- Ivor 15122012 Targeted Bill Message M2R2- Back End -->
    </INVOICE>
  </xsl:template>
  <xsl:template name="event-template-Account">
    <xsl:param name="item-list"/>
    <xsl:for-each select="$item-list/PIN_FLD_AR_ITEMS[generate-id(.) = generate-id(key('ACCOUNT-by-ACCOUNT_OBJ', substring-before(substring-after(substring-after(PIN_FLD_ACCOUNT_OBJ,' '),' '),' '))[1])]">
      <xsl:variable name="currentAccountObj" select="substring-before(substring-after(substring-after(PIN_FLD_ACCOUNT_OBJ,' '),' '),' ')"/>
      <ACCOUNT>
        <ACC_TOTAL>
          <xsl:value-of select="format-number(sum($item-list/PIN_FLD_AR_ITEMS[contains(PIN_FLD_ACCOUNT_OBJ,$currentAccountObj)][contains(PIN_FLD_SERVICE_OBJ,'0.0.0.0  0 0')]/PIN_FLD_EVENTS[not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
        </ACC_TOTAL>
        <!--  Account Level Charges  -->
        <!--  Account Level - Subscription Charges  -->
        <xsl:call-template name="event-template-Subscription-Charges">
          <xsl:with-param name="event-list" select="$item-list/PIN_FLD_AR_ITEMS[contains(PIN_FLD_ACCOUNT_OBJ,$currentAccountObj)][contains(PIN_FLD_SERVICE_OBJ,'0.0.0.0  0 0')][contains(PIN_FLD_ITEM_OBJ,'cycle')][not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]"/>
        </xsl:call-template>
        <!--  Account Level - Non-Subscription Charges  -->
        <xsl:call-template name="event-template-Non-Subscription-Charges">
          <xsl:with-param name="event-list" select="$item-list/PIN_FLD_AR_ITEMS[contains(PIN_FLD_ACCOUNT_OBJ,$currentAccountObj)][contains(PIN_FLD_SERVICE_OBJ,'0.0.0.0  0 0')][not(contains(PIN_FLD_ITEM_OBJ,'cycle'))][not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]"/>
        </xsl:call-template>
        <!--  Service Level Charges   -->
        <!--   : Loops each unique service in the respective account   -->
		<!--   Display HSI Service   -->
		<xsl:for-each select="$item-list/PIN_FLD_AR_ITEMS[generate-id(.) = generate-id(key('SERVICE-by-SERVICE_OBJ', substring-before(substring-after(substring-after(PIN_FLD_SERVICE_OBJ,' '),' '),' ')))][contains(PIN_FLD_SERVICE_OBJ,'tm_hsi')][contains(PIN_FLD_ACCOUNT_OBJ,$currentAccountObj)]">
          <xsl:sort select="(*|*/*)[name()='PIN_FLD_START_TIME']" order="ascending"/>
          <!-- Added by David to determine charge redirect and not print -->
          <xsl:variable name="currentServ" select="substring-before(substring-after(substring-after(./PIN_FLD_SERVICE_OBJ,' '),' '),' ')"/>
          <xsl:variable name="servTotalHSI">
            <xsl:value-of select="sum(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentServ)]/PIN_FLD_EVENTS[not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_BAL_IMPACTS[PIN_FLD_IMPACT_TYPE != 512 and (PIN_FLD_RESOURCE_ID = $primary-currency-id or PIN_FLD_RESOURCE_ID= 1000107)]/PIN_FLD_AMOUNT)"/>
          </xsl:variable>
          <xsl:variable name="servRedTotalHSI">
            <xsl:value-of select="sum(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentServ)][PIN_FLD_ACCOUNT_OBJ != '']/PIN_FLD_EVENTS[not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_BAL_IMPACTS[PIN_FLD_IMPACT_TYPE = 512 and (PIN_FLD_RESOURCE_ID = $primary-currency-id or PIN_FLD_RESOURCE_ID= 1000107)][PIN_FLD_AMOUNT > 0]/PIN_FLD_AMOUNT)"/>
          </xsl:variable>
          <xsl:variable name="totalHSI">
            <xsl:choose>
              <xsl:when test="$servTotalHSI &gt; 0">
                <xsl:value-of select="$servTotalHSI - $servRedTotalHSI"/>
              </xsl:when>
              <xsl:when test="$servTotalHSI &lt; 0">
                <xsl:value-of select="$servTotalHSI + $servRedTotalHSI"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="0"/>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:variable>
          <xsl:variable name="servHSICount">
            <xsl:value-of select="count(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentServ)]/PIN_FLD_EVENTS[not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_BAL_IMPACTS[not(contains(PIN_FLD_IMPACT_TYPE,'512'))][PIN_FLD_RESOURCE_ID = $primary-currency-id or PIN_FLD_RESOURCE_ID= 1000107][PIN_FLD_AMOUNT != 0]/PIN_FLD_AMOUNT)"/>
          </xsl:variable>
          <xsl:variable name="redServHSICount">
            <xsl:value-of select="count(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentServ)][PIN_FLD_ACCOUNT_OBJ != '']/PIN_FLD_EVENTS[not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_BAL_IMPACTS[contains(PIN_FLD_IMPACT_TYPE,'512')][PIN_FLD_RESOURCE_ID  = $primary-currency-id or PIN_FLD_RESOURCE_ID =1000107][PIN_FLD_AMOUNT > 0]/PIN_FLD_AMOUNT)"/>
          </xsl:variable>
          <xsl:variable name="hasServHSICharges">
            <xsl:value-of select="$servHSICount - $redServHSICount"/>
          </xsl:variable>
          <xsl:if test="$totalHSI != 0 or $hasServHSICharges &gt; 1">
            <xsl:call-template name="tm-service-section">
              <xsl:with-param name="item-service" select="."/>
            </xsl:call-template>
          </xsl:if>
        </xsl:for-each>
        <!--   Display IPTV Service   -->
        <xsl:for-each select="$item-list/PIN_FLD_AR_ITEMS[generate-id(.) = generate-id(key('SERVICE-by-SERVICE_OBJ', substring-before(substring-after(substring-after(PIN_FLD_SERVICE_OBJ,' '),' '),' ')))][contains(PIN_FLD_SERVICE_OBJ,'tm_iptv')][contains(PIN_FLD_ACCOUNT_OBJ,$currentAccountObj)]">
          <xsl:sort select="(*|*/*)[name()='PIN_FLD_START_TIME']" order="ascending"/>
          <!-- Added by David to determine charge redirect and not print -->
          <xsl:variable name="currentServ" select="substring-before(substring-after(substring-after(./PIN_FLD_SERVICE_OBJ,' '),' '),' ')"/>
          <xsl:variable name="servTotalIPTV">
            <xsl:value-of select="sum(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentServ)]/PIN_FLD_EVENTS[not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_BAL_IMPACTS[PIN_FLD_IMPACT_TYPE != 512 and (PIN_FLD_RESOURCE_ID = $primary-currency-id or PIN_FLD_RESOURCE_ID= 1000107)]/PIN_FLD_AMOUNT)"/>
          </xsl:variable>
          <xsl:variable name="servRedTotalIPTV">
            <xsl:value-of select="sum(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentServ)][PIN_FLD_ACCOUNT_OBJ != '']/PIN_FLD_EVENTS[not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_BAL_IMPACTS[PIN_FLD_IMPACT_TYPE = 512 and (PIN_FLD_RESOURCE_ID = $primary-currency-id or PIN_FLD_RESOURCE_ID= 1000107)][PIN_FLD_AMOUNT > 0]/PIN_FLD_AMOUNT)"/>
          </xsl:variable>
          <xsl:variable name="totalIPTV">
            <xsl:choose>
              <xsl:when test="$servTotalIPTV &gt; 0">
                <xsl:value-of select="$servTotalIPTV - $servRedTotalIPTV"/>
              </xsl:when>
              <xsl:when test="$servTotalIPTV &lt; 0">
                <xsl:value-of select="$servTotalIPTV + $servRedTotalIPTV"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="0"/>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:variable>
          <xsl:variable name="servIPTVCount">
            <xsl:value-of select="count(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentServ)]/PIN_FLD_EVENTS[not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_BAL_IMPACTS[not(contains(PIN_FLD_IMPACT_TYPE,'512'))][PIN_FLD_RESOURCE_ID = $primary-currency-id or PIN_FLD_RESOURCE_ID= 1000107][PIN_FLD_AMOUNT != 0]/PIN_FLD_AMOUNT)"/>
          </xsl:variable>
          <xsl:variable name="redServIPTVCount">
            <xsl:value-of select="count(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentServ)][PIN_FLD_ACCOUNT_OBJ != '']/PIN_FLD_EVENTS[not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_BAL_IMPACTS[contains(PIN_FLD_IMPACT_TYPE,'512')][PIN_FLD_RESOURCE_ID  = $primary-currency-id or PIN_FLD_RESOURCE_ID =1000107][PIN_FLD_AMOUNT > 0]/PIN_FLD_AMOUNT)"/>
          </xsl:variable>
          <xsl:variable name="hasServIPTVCharges">
            <xsl:value-of select="$servIPTVCount - $redServIPTVCount"/>
          </xsl:variable>
          <xsl:if test="$totalIPTV != 0 or $hasServIPTVCharges &gt; 1">
            <xsl:call-template name="tm-service-section">
              <xsl:with-param name="item-service" select="."/>
            </xsl:call-template>
          </xsl:if>
        </xsl:for-each>
        <!--   Display VOICE Service   -->
        <xsl:for-each select="$item-list/PIN_FLD_AR_ITEMS[generate-id(.) = generate-id(key('SERVICE-by-SERVICE_OBJ', substring-before(substring-after(substring-after(PIN_FLD_SERVICE_OBJ,' '),' '),' ')))][contains(PIN_FLD_SERVICE_OBJ,'telephony')][contains(PIN_FLD_ACCOUNT_OBJ,$currentAccountObj)]">
          <xsl:sort select="(*|*/*)[name()='PIN_FLD_START_TIME']" order="ascending"/>
          <!-- Added by David to determine charge redirect and not print -->
          <xsl:variable name="currentServ" select="substring-before(substring-after(substring-after(./PIN_FLD_SERVICE_OBJ,' '),' '),' ')"/>
          <xsl:variable name="servTotalVoice">
            <xsl:value-of select="sum(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentServ)]/PIN_FLD_EVENTS[not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_BAL_IMPACTS[PIN_FLD_IMPACT_TYPE != 512 and (PIN_FLD_RESOURCE_ID = $primary-currency-id or PIN_FLD_RESOURCE_ID= 1000107)]/PIN_FLD_AMOUNT)"/>
          </xsl:variable>
          <xsl:variable name="servRedTotalVoice">
            <xsl:value-of select="sum(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentServ)][PIN_FLD_ACCOUNT_OBJ != '']/PIN_FLD_EVENTS[not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_BAL_IMPACTS[PIN_FLD_IMPACT_TYPE = 512 and (PIN_FLD_RESOURCE_ID = $primary-currency-id or PIN_FLD_RESOURCE_ID= 1000107)][PIN_FLD_AMOUNT > 0]/PIN_FLD_AMOUNT)"/>
          </xsl:variable>
          <xsl:variable name="totalVoice">
            <xsl:choose>
              <xsl:when test="$servTotalVoice &gt; 0">
                <xsl:value-of select="$servTotalVoice - $servRedTotalVoice"/>
              </xsl:when>
              <xsl:when test="$servTotalVoice &lt; 0">
                <xsl:value-of select="$servTotalVoice + $servRedTotalVoice"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="0"/>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:variable>
          <xsl:variable name="servUsageCount">
            <xsl:value-of select="count(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentServ)]/PIN_FLD_EVENTS[not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_BAL_IMPACTS[not(contains(PIN_FLD_IMPACT_TYPE,'512'))][PIN_FLD_RESOURCE_ID = $primary-currency-id or PIN_FLD_RESOURCE_ID= 1000107][PIN_FLD_AMOUNT != 0]/PIN_FLD_AMOUNT)"/>
          </xsl:variable>
          <xsl:variable name="redServUsageCount">
            <xsl:value-of select="count(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentServ)][PIN_FLD_ACCOUNT_OBJ != '']/PIN_FLD_EVENTS[not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_BAL_IMPACTS[contains(PIN_FLD_IMPACT_TYPE,'512')][PIN_FLD_RESOURCE_ID  = $primary-currency-id or PIN_FLD_RESOURCE_ID =1000107][PIN_FLD_AMOUNT > 0]/PIN_FLD_AMOUNT)"/>
          </xsl:variable>
          <xsl:variable name="hasServUsageCharges">
            <xsl:value-of select="$servUsageCount - $redServUsageCount"/>
          </xsl:variable>
          <!-- CHARMS No: IT/CRF/1305-0144 -->
          <xsl:if test="$totalVoice != 0 or $hasServUsageCharges &gt; 1 or not(contains(./PIN_FLD_LOGIN,'#'))">
            <xsl:call-template name="tm-service-section">
              <xsl:with-param name="item-service" select="."/>
            </xsl:call-template>
          </xsl:if>
        </xsl:for-each>
        
        <!-- Display Network and Leg Grouping -->
        <xsl:for-each select="/invoice/TM_FLD_CUST_SRV_NW_PROFILE/PIN_FLD_SERVICES[generate-id(.) = generate-id(key('NETWORK-by-SERVICE_OBJ', substring-before(substring-after(substring-after(PIN_FLD_SERVICE_OBJ,' '),' '),' ')))]">
            <xsl:sort select="(*|*/*)[name()='PIN_FLD_ORDER_COUNT']" order="ascending"/>
            <xsl:sort select="(*|*/*)[name()='PIN_FLD_LOGIN']" order="ascending"/>
            <!-- Added by David to determine charge redirect and not print -->
          <xsl:variable name="netwServ" select="./PIN_FLD_SERVICE_OBJ"/> 

	      <xsl:call-template name="tm-network-service">
          <xsl:with-param name="serv-poid" select="$netwServ"/>
          </xsl:call-template>
          
        </xsl:for-each>
        
        <!--   Display Other Services   -->
        <xsl:for-each select="$item-list/PIN_FLD_AR_ITEMS[generate-id(.) = generate-id(key('SERVICE-by-SERVICE_OBJ', substring-before(substring-after(substring-after(PIN_FLD_SERVICE_OBJ,' '),' '),' ')))][not(contains(PIN_FLD_SERVICE_OBJ,'tm_hsi')) and not(contains(PIN_FLD_SERVICE_OBJ,'tm_iptv')) and not(contains(PIN_FLD_SERVICE_OBJ,'telephony')) and not(contains(PIN_FLD_SERVICE_OBJ,'0.0.0.0  0 0'))][contains(PIN_FLD_ACCOUNT_OBJ,$currentAccountObj)]">
          <xsl:sort select="(*|*/*)[name()='PIN_FLD_ORDER_COUNT']" order="ascending"/>
          <!-- <xsl:sort select="(*|*/*)[name()='PIN_FLD_START_TIME']" order="ascending"/> --> <!-- For BE774 sorting -->
          <xsl:sort select="(*|*/*)[name()='PIN_FLD_LOGIN']" order="ascending"/>
          <!-- Added by David to determine charge redirect and not print -->
          <xsl:variable name="currentServ" select="substring-before(substring-after(substring-after(./PIN_FLD_SERVICE_OBJ,' '),' '),' ')"/>
          <xsl:variable name="currentActServ" select="./PIN_FLD_SERVICE_OBJ"/>
          <xsl:variable name="servTotalOther">
            <xsl:value-of select="sum(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentServ)]/PIN_FLD_EVENTS[not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_BAL_IMPACTS[PIN_FLD_IMPACT_TYPE != 512 and (PIN_FLD_RESOURCE_ID = $primary-currency-id or PIN_FLD_RESOURCE_ID= 1000107)]/PIN_FLD_AMOUNT)"/>
          </xsl:variable>
          <xsl:variable name="servRedTotalOther">
            <xsl:value-of select="sum(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentServ)][PIN_FLD_ACCOUNT_OBJ != '']/PIN_FLD_EVENTS[not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_BAL_IMPACTS[PIN_FLD_IMPACT_TYPE = 512 and (PIN_FLD_RESOURCE_ID = $primary-currency-id or PIN_FLD_RESOURCE_ID= 1000107)][PIN_FLD_AMOUNT > 0]/PIN_FLD_AMOUNT)"/>
          </xsl:variable>
          <xsl:variable name="servBurstTotal">
            <xsl:value-of select="sum(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentServ)]/PIN_FLD_EVENTS[contains(PIN_FLD_ITEM_OBJ,'burstable')]/PIN_FLD_TOTAL[@elem = $primary-currency-id]/PIN_FLD_AMOUNT)"/>
          </xsl:variable>
          <xsl:variable name="totalOther">
            <xsl:choose>
              <xsl:when test="$servTotalOther &gt; 0">
                <xsl:value-of select="$servTotalOther - $servRedTotalOther"/>
              </xsl:when>
              <xsl:when test="$servTotalOther &lt; 0">
                <xsl:value-of select="$servTotalOther + $servRedTotalOther"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="0"/>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:variable>
          <xsl:variable name="servOtherCount">
            <xsl:value-of select="count(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentServ)]/PIN_FLD_EVENTS[not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_BAL_IMPACTS[not(contains(PIN_FLD_IMPACT_TYPE,'512'))][PIN_FLD_RESOURCE_ID = $primary-currency-id or PIN_FLD_RESOURCE_ID= 1000107][PIN_FLD_AMOUNT != 0]/PIN_FLD_AMOUNT)"/>
          </xsl:variable>
          <xsl:variable name="redServOtherCount">
            <xsl:value-of select="count(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentServ)][PIN_FLD_ACCOUNT_OBJ != '']/PIN_FLD_EVENTS[not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_BAL_IMPACTS[contains(PIN_FLD_IMPACT_TYPE,'512')][PIN_FLD_RESOURCE_ID  = $primary-currency-id or PIN_FLD_RESOURCE_ID =1000107][PIN_FLD_AMOUNT > 0]/PIN_FLD_AMOUNT)"/>
          </xsl:variable>
          <xsl:variable name="hasServOtherCharges">
            <xsl:value-of select="$servOtherCount - $redServOtherCount"/>
          </xsl:variable>
          <xsl:if test="$totalOther + $servBurstTotal != 0 or $hasServOtherCharges &gt; 1">
            <!-- Leg service : Do nothing -->
            <xsl:if test="/invoice/TM_FLD_CUST_SRV_NW_PROFILE/PIN_FLD_SERVICES/PIN_FLD_SERVICES[contains(PIN_FLD_SERVICE_OBJ,$currentServ)]">
              <!-- DO NOTHING -->
            </xsl:if>
            
              	<xsl:variable name="legNetw" select="/invoice/TM_FLD_CUST_SRV_ATT_PROFILE/PIN_FLD_SERVICES[contains(PIN_FLD_SERVICE_OBJ,$currentServ)]/PIN_FLD_NETWORK_ELEMENT_INFO/PIN_FLD_NETWORK_ID"/>
              	  <xsl:variable name="legNoProfile">
				    <xsl:choose>
				      <xsl:when test="/invoice/TM_FLD_CUST_SRV_NW_PROFILE/PIN_FLD_SERVICES/PIN_FLD_NETWORK_ELEMENT_INFO/PIN_FLD_NETWORK_ID = $legNetw">
				        <xsl:value-of select="$legNetw"/>
				      </xsl:when>
				      <xsl:otherwise>LegNoProfile</xsl:otherwise>
				    </xsl:choose>
				  </xsl:variable>
            <!-- Network Service: Do Nothing -->
            <xsl:if test="/invoice/TM_FLD_CUST_SRV_NW_PROFILE/PIN_FLD_SERVICES[contains(PIN_FLD_SERVICE_OBJ,$currentServ)]">
              <!-- DO NOTHING -->
            </xsl:if>
            <!-- If not network and leg tm-service-section -->
            <xsl:if test="not(/invoice/TM_FLD_CUST_SRV_NW_PROFILE/PIN_FLD_SERVICES/PIN_FLD_SERVICE_OBJ = $currentActServ)">
              <xsl:if test="not(/invoice/TM_FLD_CUST_SRV_NW_PROFILE/PIN_FLD_SERVICES/PIN_FLD_SERVICES/PIN_FLD_SERVICE_OBJ = $currentActServ)">
                 <xsl:if test="not(contains($currentActServ,'leg')) or $legNoProfile = 'LegNoProfile'">
	                <xsl:call-template name="tm-service-section">
	                  <xsl:with-param name="item-service" select="."/>
	                </xsl:call-template>
	             </xsl:if>
              </xsl:if>
            </xsl:if>
          </xsl:if>
        </xsl:for-each>
      </ACCOUNT>
    </xsl:for-each>
  </xsl:template>
  
  
  <xsl:template name="tm-network-service-null">
    <xsl:param name="serv-poid"/>
    <xsl:variable name="networkServ" select="$serv-poid"/>
    <xsl:if test="/invoice/TM_FLD_CUST_SRV_NW_PROFILE/PIN_FLD_SERVICES[contains(PIN_FLD_SERVICE_OBJ,$networkServ)]">
      <SERVICE>
        <SERVICE_NAME>
          <xsl:value-of select="./PIN_FLD_SERVICE_ID"/>
        </SERVICE_NAME>
        <xsl:if test="substring(./PIN_FLD_LOGIN,1,2)='60'">
          <SERVICE_IDENTIFIER>
            <xsl:value-of select="concat(substring(./PIN_FLD_LOGIN,2,2),'-',substring(./PIN_FLD_LOGIN,4))"/>
          </SERVICE_IDENTIFIER>
        </xsl:if>
        <xsl:if test="substring(./PIN_FLD_LOGIN,1,2)!='60'">
          <SERVICE_IDENTIFIER>
            <xsl:value-of select="./PIN_FLD_LOGIN"/>
          </SERVICE_IDENTIFIER>
        </xsl:if>
        <SERVICE_AMT_TAXABLE>0.00</SERVICE_AMT_TAXABLE>
        <SERVICE_AMT_TAX>0.00</SERVICE_AMT_TAX>
        <SERVICE_TOTAL>0.00</SERVICE_TOTAL>
      </SERVICE>
    </xsl:if>
  </xsl:template>
  
  
  
  <xsl:template name="tm-network-service">
    <xsl:param name="serv-poid"/>
    <xsl:variable name="networkServ" select="$serv-poid"/>
    <xsl:variable name="networkId" select="/invoice/TM_FLD_CUST_SRV_NW_PROFILE/PIN_FLD_SERVICES[contains(PIN_FLD_SERVICE_OBJ,$networkServ)]/PIN_FLD_NETWORK_ELEMENT_INFO/PIN_FLD_NETWORK_ID"/>

    <!-- Display network service first -->
    <xsl:if test="/invoice/PIN_FLD_AR_ITEMS/PIN_FLD_SERVICE_OBJ = $networkServ">

      <xsl:variable name="netwServTotalOther">
        <xsl:value-of select="sum(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$networkServ)]/PIN_FLD_EVENTS[not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_BAL_IMPACTS[PIN_FLD_IMPACT_TYPE != 512 and (PIN_FLD_RESOURCE_ID = $primary-currency-id or PIN_FLD_RESOURCE_ID= 1000107)]/PIN_FLD_AMOUNT)"/>
      </xsl:variable>
      <xsl:variable name="netwServRedTotalOther">
        <xsl:value-of select="sum(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$networkServ)][PIN_FLD_ACCOUNT_OBJ != '']/PIN_FLD_EVENTS[not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_BAL_IMPACTS[PIN_FLD_IMPACT_TYPE = 512 and (PIN_FLD_RESOURCE_ID = $primary-currency-id or PIN_FLD_RESOURCE_ID= 1000107)][PIN_FLD_AMOUNT > 0]/PIN_FLD_AMOUNT)"/>
      </xsl:variable>
      <xsl:variable name="netwTotalOther">
        <xsl:choose>
          <xsl:when test="$netwServTotalOther &gt; 0">
            <xsl:value-of select="$netwServTotalOther - $netwServRedTotalOther"/>
          </xsl:when>
          <xsl:when test="$netwServTotalOther &lt; 0">
            <xsl:value-of select="$netwServTotalOther + $netwServRedTotalOther"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="0"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:variable name="netwServOtherCount">
        <xsl:value-of select="count(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$networkServ)]/PIN_FLD_EVENTS[not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_BAL_IMPACTS[not(contains(PIN_FLD_IMPACT_TYPE,'512'))][PIN_FLD_RESOURCE_ID = $primary-currency-id or PIN_FLD_RESOURCE_ID= 1000107][PIN_FLD_AMOUNT != 0]/PIN_FLD_AMOUNT)"/>
      </xsl:variable>
      <xsl:variable name="netwRedServOtherCount">
        <xsl:value-of select="count(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$networkServ)][PIN_FLD_ACCOUNT_OBJ != '']/PIN_FLD_EVENTS[not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_BAL_IMPACTS[contains(PIN_FLD_IMPACT_TYPE,'512')][PIN_FLD_RESOURCE_ID  = $primary-currency-id or PIN_FLD_RESOURCE_ID =1000107][PIN_FLD_AMOUNT > 0]/PIN_FLD_AMOUNT)"/>
      </xsl:variable>
      <xsl:variable name="netwHasServOtherCharges">
        <xsl:value-of select="$netwServOtherCount - $netwRedServOtherCount"/>
      </xsl:variable>
	
	  <xsl:if test="$netwTotalOther != 0 or $netwHasServOtherCharges &gt; 1">
	  
		<xsl:call-template name="tm-service-section">
		  <xsl:with-param name="item-service" select="/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$networkServ)]"/>
		</xsl:call-template>
      </xsl:if>
      
      <xsl:if test="$netwTotalOther = 0 or $netwHasServOtherCharges &lt; 1">
            <!-- To check if belong to network service and has no charges, to still print header -->
            <xsl:call-template name="tm-network-service-null">
              <xsl:with-param name="serv-poid" select="$networkServ"/>
           </xsl:call-template>
      </xsl:if>
    </xsl:if>
    <!-- Loop leg services -->
    <!-- <xsl:for-each select="/invoice/TM_FLD_CUST_SRV_NW_PROFILE/PIN_FLD_SERVICES[contains(PIN_FLD_SERVICE_OBJ,$networkServ)]/PIN_FLD_SERVICES"> -->
    <xsl:for-each select="/invoice/TM_FLD_CUST_SRV_ATT_PROFILE/PIN_FLD_SERVICES[contains(PIN_FLD_NETWORK_ELEMENT_INFO/PIN_FLD_NETWORK_ID,$networkId)]">
      <!-- Display Leg Services -->
      <xsl:variable name="legServ" select="substring-before(substring-after(substring-after(PIN_FLD_SERVICE_OBJ,' '),' '),' ')"/>
      <xsl:variable name="servTotalOther">
        <xsl:value-of select="sum(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$legServ)]/PIN_FLD_EVENTS[not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_BAL_IMPACTS[PIN_FLD_IMPACT_TYPE != 512 and (PIN_FLD_RESOURCE_ID = $primary-currency-id or PIN_FLD_RESOURCE_ID= 1000107)]/PIN_FLD_AMOUNT)"/>
      </xsl:variable>
      <xsl:variable name="servRedTotalOther">
        <xsl:value-of select="sum(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$legServ)][PIN_FLD_ACCOUNT_OBJ != '']/PIN_FLD_EVENTS[not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_BAL_IMPACTS[PIN_FLD_IMPACT_TYPE = 512 and (PIN_FLD_RESOURCE_ID = $primary-currency-id or PIN_FLD_RESOURCE_ID= 1000107)][PIN_FLD_AMOUNT > 0]/PIN_FLD_AMOUNT)"/>
      </xsl:variable>
      <xsl:variable name="totalOther">
        <xsl:choose>
          <xsl:when test="$servTotalOther &gt; 0">
            <xsl:value-of select="$servTotalOther - $servRedTotalOther"/>
          </xsl:when>
          <xsl:when test="$servTotalOther &lt; 0">
            <xsl:value-of select="$servTotalOther + $servRedTotalOther"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="0"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:variable name="servOtherCount">
        <xsl:value-of select="count(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$legServ)]/PIN_FLD_EVENTS[not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_BAL_IMPACTS[not(contains(PIN_FLD_IMPACT_TYPE,'512'))][PIN_FLD_RESOURCE_ID = $primary-currency-id or PIN_FLD_RESOURCE_ID= 1000107][PIN_FLD_AMOUNT != 0]/PIN_FLD_AMOUNT)"/>
      </xsl:variable>
      <xsl:variable name="redServOtherCount">
        <xsl:value-of select="count(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$legServ)][PIN_FLD_ACCOUNT_OBJ != '']/PIN_FLD_EVENTS[not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_BAL_IMPACTS[contains(PIN_FLD_IMPACT_TYPE,'512')][PIN_FLD_RESOURCE_ID  = $primary-currency-id or PIN_FLD_RESOURCE_ID =1000107][PIN_FLD_AMOUNT > 0]/PIN_FLD_AMOUNT)"/>
      </xsl:variable>
      <xsl:variable name="hasServOtherCharges">
        <xsl:value-of select="$servOtherCount - $redServOtherCount"/>
      </xsl:variable>
	  <!--  William - 2014/05/07 - Make the changes to match 2 condition from or -> and -->
      <xsl:if test="$totalOther != 0 or $hasServOtherCharges != 0">
        <xsl:call-template name="tm-service-section">
          <xsl:with-param name="item-service" select="/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$legServ)]"/>
        </xsl:call-template>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>
  <xsl:template name="tm-service-section">
    <xsl:param name="item-service"/>
    <!--  Passes in only service obj since at this stage; service obj is already pointing to a particular account  -->
    <xsl:variable name="currentService" select="substring-before(substring-after(substring-after($item-service/PIN_FLD_SERVICE_OBJ,' '),' '),' ')"/>
    <xsl:variable name="networkId" select="/invoice/TM_FLD_CUST_SRV_ATT_PROFILE/PIN_FLD_SERVICES[contains(PIN_FLD_SERVICE_OBJ,$currentService)]/PIN_FLD_NETWORK_ELEMENT_INFO/PIN_FLD_NETWORK_ID"/>
    <xsl:variable name="netwlogin" select="/invoice/TM_FLD_CUST_SRV_NW_PROFILE/PIN_FLD_SERVICES[PIN_FLD_NETWORK_ELEMENT_INFO/PIN_FLD_NETWORK_ID=$networkId]/PIN_FLD_LOGIN"/>
    <SERVICE>
      <SERVICE_NAME>
        <xsl:value-of select="$item-service/PIN_FLD_SERVICE_ID"/>
      </SERVICE_NAME>
      <xsl:if test="substring($item-service/PIN_FLD_LOGIN,1,2)='60'">
        <SERVICE_IDENTIFIER>
          <xsl:value-of select="concat(substring($item-service/PIN_FLD_LOGIN,2,2),'-',substring($item-service/PIN_FLD_LOGIN,4))"/>
          <xsl:if test="$networkId != ''"> (Network ID: <xsl:value-of select="$netwlogin"/>)</xsl:if>
        </SERVICE_IDENTIFIER>
      </xsl:if>
      <xsl:if test="substring($item-service/PIN_FLD_LOGIN,1,2)!='60'">
        <SERVICE_IDENTIFIER>
          <xsl:value-of select="$item-service/PIN_FLD_LOGIN"/>
          <xsl:if test="$networkId != ''"> (Network ID: <xsl:value-of select="$netwlogin"/>)</xsl:if>
        </SERVICE_IDENTIFIER>
      </xsl:if>
      <!-- Service ATTR -->
      <xsl:call-template name="service-template-service-attr">
        <xsl:with-param name="serv-poid" select="$currentService"/>
      </xsl:call-template>
      <!-- Added by David for exclusion of Charge Sharing -->
      <xsl:variable name="allServiceTaxable">
        <xsl:value-of select="sum(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentService)]/PIN_FLD_EVENTS/PIN_FLD_BAL_IMPACTS[PIN_FLD_IMPACT_TYPE != 4 and PIN_FLD_IMPACT_TYPE != 512 and PIN_FLD_TAX_CODE != '' and PIN_FLD_TAX_CODE != 'ZERO' and (PIN_FLD_TAX_CODE != 'NORM' or PIN_FLD_RESOURCE_ID= 1000107) and (PIN_FLD_RESOURCE_ID = $primary-currency-id or PIN_FLD_RESOURCE_ID= 1000107)]/PIN_FLD_AMOUNT)"/>
      </xsl:variable>
      <xsl:variable name="redServiceTaxable">
        <xsl:value-of select="sum(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentService)][PIN_FLD_ACCOUNT_OBJ != '']/PIN_FLD_EVENTS/PIN_FLD_BAL_IMPACTS[PIN_FLD_IMPACT_TYPE != 4 and PIN_FLD_IMPACT_TYPE = 512 and PIN_FLD_TAX_CODE != '' and PIN_FLD_TAX_CODE != 'ZERO' and (PIN_FLD_TAX_CODE != 'NORM' or PIN_FLD_RESOURCE_ID= 1000107) and (PIN_FLD_RESOURCE_ID = $primary-currency-id or PIN_FLD_RESOURCE_ID= 1000107)][PIN_FLD_AMOUNT > 0]/PIN_FLD_AMOUNT)"/>
      </xsl:variable>
      <xsl:variable name="totalServiceTaxable">
        <xsl:choose>
          <xsl:when test="$allServiceTaxable &gt; 0">
            <xsl:value-of select="$allServiceTaxable - $redServiceTaxable"/>
          </xsl:when>
          <xsl:when test="$allServiceTaxable &lt; 0">
            <xsl:value-of select="$allServiceTaxable + $redServiceTaxable"/>
          </xsl:when>
          <xsl:when test="$allServiceTaxable = 0 and $redServiceTaxable = 0">
            <xsl:value-of select="0"/>
          </xsl:when>
        </xsl:choose>
      </xsl:variable>
      <SERVICE_AMT_TAXABLE>
        <xsl:value-of select="format-number($totalServiceTaxable,'###,###,###,##0.00')"/>
      </SERVICE_AMT_TAXABLE>
      <xsl:variable name="unrounded-Tax">
        <xsl:value-of select="$totalServiceTaxable*$taxRate"/>
      </xsl:variable>
      <!--   END   -->
      <SERVICE_AMT_TAX>
        <xsl:value-of select="format-number(round(100*$unrounded-Tax) div 100,'###,###,###,##0.00')"/>
      </SERVICE_AMT_TAX>
      <SERVICE_TOTAL>
        <xsl:variable name="burstable-amt">
          <xsl:value-of select="sum(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentService)]/PIN_FLD_EVENTS[contains(PIN_FLD_ITEM_OBJ,'burstable')][PIN_FLD_DESCR='BURST']/PIN_FLD_TOTAL/PIN_FLD_AMOUNT)"/>
        </xsl:variable>
        <xsl:choose>
          <xsl:when test="$allServiceTaxable &gt; 0">
            <xsl:value-of select="format-number(sum(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentService)]/PIN_FLD_EVENTS[not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID = $primary-currency-id or PIN_FLD_RESOURCE_ID= 1000107]/PIN_FLD_AMOUNT) - $redServiceTaxable + (round(100*$unrounded-Tax) div 100) + $burstable-amt,'###,###,###,##0.00')"/>
          </xsl:when>
          <xsl:when test="$allServiceTaxable &lt; 0">
            <xsl:value-of select="format-number(sum(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentService)]/PIN_FLD_EVENTS[not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID = $primary-currency-id or PIN_FLD_RESOURCE_ID= 1000107]/PIN_FLD_AMOUNT) + $redServiceTaxable + (round(100*$unrounded-Tax) div 100) + $burstable-amt,'###,###,###,##0.00')"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="format-number(sum(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentService)]/PIN_FLD_EVENTS[not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID = $primary-currency-id or PIN_FLD_RESOURCE_ID= 1000107]/PIN_FLD_AMOUNT) + (round(100*$unrounded-Tax) div 100) + $burstable-amt,'###,###,###,##0.00')"/>
          </xsl:otherwise>
        </xsl:choose>
      </SERVICE_TOTAL>
      <xsl:variable name="allUsgTaxable">
        <xsl:value-of select="sum(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentService)]/PIN_FLD_EVENTS[contains(PIN_FLD_ITEM_OBJ,'usage') or contains(PIN_FLD_ITEM_OBJ,'iptv')]/PIN_FLD_BAL_IMPACTS[PIN_FLD_IMPACT_TYPE != 4 and PIN_FLD_IMPACT_TYPE != 512 and PIN_FLD_TAX_CODE != '' and (PIN_FLD_RESOURCE_ID = $primary-currency-id or PIN_FLD_RESOURCE_ID= 1000107)]/PIN_FLD_AMOUNT)"/>
      </xsl:variable>
      <xsl:variable name="redUsgTaxable">
        <xsl:value-of select="sum(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentService)][PIN_FLD_ACCOUNT_OBJ != '']/PIN_FLD_EVENTS[contains(PIN_FLD_ITEM_OBJ,'usage') or contains(PIN_FLD_ITEM_OBJ,'iptv')]/PIN_FLD_BAL_IMPACTS[PIN_FLD_IMPACT_TYPE != 4 and PIN_FLD_IMPACT_TYPE = 512 and PIN_FLD_TAX_CODE != '' and (PIN_FLD_RESOURCE_ID = $primary-currency-id or PIN_FLD_RESOURCE_ID= 1000107)][PIN_FLD_AMOUNT > 0]/PIN_FLD_AMOUNT)"/>
      </xsl:variable>
      <xsl:variable name="totalUsgTaxable">
        <xsl:choose>
          <xsl:when test="$allUsgTaxable &gt; 0">
            <xsl:value-of select="$allUsgTaxable - $redUsgTaxable"/>
          </xsl:when>
          <xsl:when test="$allUsgTaxable &lt; 0">
            <xsl:value-of select="$allUsgTaxable + $redUsgTaxable"/>
          </xsl:when>
          <xsl:when test="$allUsgTaxable = 0 and $redUsgTaxable = 0">
            <xsl:value-of select="0"/>
          </xsl:when>
        </xsl:choose>
      </xsl:variable>
      <xsl:variable name="hasUsages">
        <xsl:value-of select="count(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentService)]/PIN_FLD_EVENTS[contains(PIN_FLD_ITEM_OBJ,'usage') or contains(PIN_FLD_ITEM_OBJ,'iptv')]/PIN_FLD_BAL_IMPACTS[PIN_FLD_IMPACT_TYPE != 4 and PIN_FLD_IMPACT_TYPE != 512 and PIN_FLD_TAX_CODE != ''][PIN_FLD_AMOUNT != 0]/PIN_FLD_AMOUNT)"/>
      </xsl:variable>
      <xsl:if test="$hasUsages &gt; 1">
        <USG_TOTAL>
          <xsl:value-of select="format-number($totalUsgTaxable,'###,###,###,##0.00')"/>
        </USG_TOTAL>
      </xsl:if>
      <!--  Service Level - Subscription Charges  -->
      <xsl:variable name="sumServiceRC">
        <xsl:value-of select="sum(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentService)][contains(PIN_FLD_ITEM_OBJ,'cycle')][not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_EVENTS/PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT)"/>
      </xsl:variable>
      <!-- amer 20120605 dont display this RC_CHARGES if it dont have any RC_ITEM -->
      <xsl:if test="not(contains(/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP,'GLOBAL') and format-number($sumServiceRC,'###,###,###,##0.00') = '0.00')">
        <xsl:call-template name="event-template-Subscription-Charges">
          <xsl:with-param name="event-list" select="/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentService)][contains(PIN_FLD_ITEM_OBJ,'cycle')][not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]"/>
        </xsl:call-template>
      </xsl:if>
      <!-- amer 20120605 dont display this RC_CHARGES if it dont have any RC_ITEM end-->
      <!--  Service Level - Non-Subscription Charges  -->
      <xsl:call-template name="event-template-Non-Subscription-Charges">
        <xsl:with-param name="event-list" select="/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentService)][not(contains(PIN_FLD_ITEM_OBJ,'cycle'))][not(contains(PIN_FLD_ITEM_OBJ,'usage'))][not(contains(PIN_FLD_ITEM_OBJ,'iptv'))][not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))][not(contains(PIN_FLD_ITEM_OBJ,'burstable'))]"/>
      </xsl:call-template>
      <!--  Service Level - Usage Charges  -->
      <!-- amer 20130129 call the free minute template -->
      <xsl:call-template name="idd-free-minute-template">
        <xsl:with-param name="serv-poid" select="$currentService"/>
      </xsl:call-template>
      <!--  Below if added to not create USG_CHARGES tag if USG_TOTAL is 0 due to BPM issues  -->
      <!-- Added by David to still show if usages are discounted to 0 via variable hasUsages -->
      <xsl:if test="$totalUsgTaxable != 0 or $hasUsages &gt; 1">
        <xsl:call-template name="event-template-Usage-Charges">
          <xsl:with-param name="event-list" select="key('SERVICE-by-SERVICE_OBJ', $currentService)[contains(PIN_FLD_ITEM_OBJ,'usage')]"/>
        </xsl:call-template>
        <xsl:call-template name="event-template-Usage-Charges">
          <xsl:with-param name="event-list" select="key('SERVICE-by-SERVICE_OBJ', $currentService)[contains(PIN_FLD_ITEM_OBJ,'iptv')]"/>
        </xsl:call-template>
      </xsl:if>
      <!-- amer 20130902 burstable usage -->
      <xsl:if test="/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentService)]/PIN_FLD_EVENTS[contains(PIN_FLD_ITEM_OBJ,'burstable')][PIN_FLD_DESCR='BURST']">
      <xsl:call-template name="event-template-Usage-Charges-burstable">
          <xsl:with-param name="event-list" select="/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentService)]/PIN_FLD_EVENTS[contains(PIN_FLD_ITEM_OBJ,'burstable')][PIN_FLD_DESCR='BURST']"/>
        </xsl:call-template>
      </xsl:if>
    </SERVICE>
  </xsl:template>
  <xsl:template name="format-idd-minute">
  <xsl:param name="idd-second"/>
  <xsl:variable name="in-minute">
	  <xsl:value-of select="$idd-second div 60" />
  </xsl:variable>
	  <xsl:choose>
		<xsl:when test="$in-minute &lt; 0"><xsl:value-of select="format-number($in-minute * -1,'###,###,###,##0.0')" /></xsl:when>
		<xsl:when test="$in-minute &gt; 0"><xsl:value-of select="format-number($in-minute,'###,###,###,##0.0')" /></xsl:when>
		<xsl:otherwise><xsl:value-of select="format-number(0,'###,###,###,##0.0')" /></xsl:otherwise>
	  </xsl:choose>
  </xsl:template>
  <!-- amer 20130129 add template to check if this service have free minute -->
  <xsl:template name="idd-free-minute-template">
    <xsl:param name="serv-poid"/>
    <xsl:if test="/invoice/PIN_FLD_OTHER_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$serv-poid) and PIN_FLD_NAME='1000141']">
    <FREE_MINUTE>
      <xsl:for-each select="/invoice/PIN_FLD_OTHER_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$serv-poid) and PIN_FLD_NAME='1000141']">
      <FM_ITEM>
		<DESC><xsl:value-of select="PIN_FLD_DESCR" /></DESC>
		<ITEM_START_DATE><xsl:call-template name="format-unix-date"><xsl:with-param name="unix" select="PIN_FLD_START_T"/></xsl:call-template></ITEM_START_DATE>
		<ITEM_END_DATE><xsl:call-template name="format-unix-date"><xsl:with-param name="unix" select="PIN_FLD_END_T - 86400"/></xsl:call-template></ITEM_END_DATE>
		<MINUTE_FULL><xsl:call-template name="format-idd-minute"><xsl:with-param name="idd-second" select="PIN_FLD_SCALED_AMOUNT"/></xsl:call-template></MINUTE_FULL>		
		<MINUTE_USED><xsl:call-template name="format-idd-minute"><xsl:with-param name="idd-second" select="(PIN_FLD_SCALED_AMOUNT - PIN_FLD_CURRENT_BAL)"/></xsl:call-template></MINUTE_USED>
	  </FM_ITEM>
      </xsl:for-each>
      <MINUTE_FULL_TOTAL><xsl:call-template name="format-idd-minute"><xsl:with-param name="idd-second" select="sum(/invoice/PIN_FLD_OTHER_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$serv-poid) and PIN_FLD_NAME='1000141']/PIN_FLD_SCALED_AMOUNT)"/></xsl:call-template></MINUTE_FULL_TOTAL>
      <MINUTE_USED_TOTAL><xsl:call-template name="format-idd-minute"><xsl:with-param name="idd-second" select="(sum(/invoice/PIN_FLD_OTHER_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$serv-poid) and PIN_FLD_NAME='1000141']/PIN_FLD_SCALED_AMOUNT) - sum(/invoice/PIN_FLD_OTHER_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$serv-poid) and PIN_FLD_NAME='1000141']/PIN_FLD_CURRENT_BAL))"/></xsl:call-template></MINUTE_USED_TOTAL>
    </FREE_MINUTE>
    </xsl:if>
  </xsl:template>
  <!-- amer 20130129 add template to check if this service have free minute end -->
  <xsl:template name="event-template-Subscription-Charges-Summary-Network">
    <xsl:param name="serv-poid"/>
    <xsl:variable name="networkServ" select="$serv-poid"/>
    <xsl:variable name="networkId" select="/invoice/TM_FLD_CUST_SRV_NW_PROFILE/PIN_FLD_SERVICES[contains(PIN_FLD_SERVICE_OBJ,$networkServ)]/PIN_FLD_NETWORK_ELEMENT_INFO/PIN_FLD_NETWORK_ID"/>
    <!-- Display network service first - if exists (2013-05-14)-->
    <xsl:if test="/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$networkServ)][contains(PIN_FLD_ITEM_OBJ,'cycle_forward')]">
	    <xsl:call-template name="event-template-Subscription-Charges-Summary">
	      <xsl:with-param name="event-list" select="/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$networkServ)]"/>
	    </xsl:call-template>
	</xsl:if>
    <!-- Loop leg services -->
    <!-- <xsl:for-each select="/invoice/TM_FLD_CUST_SRV_NW_PROFILE/PIN_FLD_SERVICES[contains(PIN_FLD_SERVICE_OBJ,$networkServ)]/PIN_FLD_SERVICES"> -->
    <xsl:for-each select="/invoice/TM_FLD_CUST_SRV_ATT_PROFILE/PIN_FLD_SERVICES[contains(PIN_FLD_NETWORK_ELEMENT_INFO/PIN_FLD_NETWORK_ID,$networkId)]">
      <!-- Display Leg Services -->
      <xsl:variable name="legServ" select="substring-before(substring-after(substring-after(PIN_FLD_SERVICE_OBJ,' '),' '),' ')"/>
      <xsl:if test="/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$legServ)][contains(PIN_FLD_ITEM_OBJ,'cycle_forward')]">
        <xsl:call-template name="event-template-Subscription-Charges-Summary">
          <xsl:with-param name="event-list" select="/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$legServ)]"/>
        </xsl:call-template>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>
  <xsl:template name="event-template-Subscription-Charges-Summary">
    <xsl:param name="event-list"/>
    <xsl:variable name="currentService" select="substring-before(substring-after(substring-after($event-list/PIN_FLD_SERVICE_OBJ,' '),' '),' ')"/>
    <xsl:variable name="networkId" select="/invoice/TM_FLD_CUST_SRV_ATT_PROFILE/PIN_FLD_SERVICES[contains(PIN_FLD_SERVICE_OBJ,$currentService)]/PIN_FLD_NETWORK_ELEMENT_INFO/PIN_FLD_NETWORK_ID"/>
    <xsl:variable name="netwlogin" select="/invoice/TM_FLD_CUST_SRV_NW_PROFILE/PIN_FLD_SERVICES[PIN_FLD_NETWORK_ELEMENT_INFO/PIN_FLD_NETWORK_ID=$networkId]/PIN_FLD_LOGIN"/>
    <xsl:if test="/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentService)][contains(PIN_FLD_ITEM_OBJ,'cycle')][not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_EVENTS">
      <!-- amer - 20110919 - remove items with 0 charge for global - start 02 -->
      <xsl:variable name="netrcamount" select="format-number(sum(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentService)][contains(PIN_FLD_ITEM_OBJ,'cycle')][not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_EVENTS/PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
      <xsl:if test="not(contains(/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP,'GLOBAL') and $netrcamount = '0.00') ">
        <RC_ITEM>
          <xsl:if test="substring($event-list/PIN_FLD_LOGIN,1,2)='60'">
            <DESC>
              <xsl:value-of select="$event-list/PIN_FLD_SERVICE_ID"/> : <xsl:value-of select="concat(substring($event-list/PIN_FLD_LOGIN,2,2),'-',substring($event-list/PIN_FLD_LOGIN,4))"/>
              <xsl:if test="$networkId != ''"> (Network Id: <xsl:value-of select="$netwlogin"/>)</xsl:if>
            </DESC>
          </xsl:if>
          <xsl:if test="substring($event-list/PIN_FLD_LOGIN,1,2)!='60'">
            <DESC>
              <xsl:value-of select="$event-list/PIN_FLD_SERVICE_ID"/> : <xsl:value-of select="$event-list/PIN_FLD_LOGIN"/>
              <xsl:if test="$networkId != ''"> (Network ID: <xsl:value-of select="$netwlogin"/>)</xsl:if>
            </DESC>
          </xsl:if>
          <xsl:choose>
            <xsl:when test="contains(/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP,'WHOLESALE') or contains(/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP,'GLOBAL')">
              <AMT_GROSS/>
              <AMT_DISCOUNT/>
            </xsl:when>
            <xsl:otherwise>
              <AMT_GROSS>
                <xsl:value-of select="format-number(sum(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentService)][contains(PIN_FLD_ITEM_OBJ,'cycle')][not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_EVENTS/PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id][PIN_FLD_IMPACT_TYPE != 128 and PIN_FLD_IMPACT_TYPE != 512]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
              </AMT_GROSS>
              <AMT_DISCOUNT>
                <xsl:value-of select="format-number(sum(/invoice/PIN_FLD_AR_ITEMS[contains(PIN_FLD_SERVICE_OBJ,$currentService)][contains(PIN_FLD_ITEM_OBJ,'cycle')][not(contains(PIN_FLD_ITEM_OBJ,'cycle_tax'))]/PIN_FLD_EVENTS/PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id][PIN_FLD_IMPACT_TYPE = 128]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
              </AMT_DISCOUNT>
            </xsl:otherwise>
          </xsl:choose>
          <AMT_NETT>
            <xsl:value-of select="$netrcamount"/>
          </AMT_NETT>
        </RC_ITEM>
      </xsl:if>
      <!-- amer - 20110919 - remove items with 0 charge for global - end 02 -->
    </xsl:if>
  </xsl:template>
  <xsl:template name="service-template-service-attr">
    <xsl:param name="serv-poid"/>
    <xsl:if test="/invoice/TM_FLD_CUST_SRV_ATT_PROFILE/PIN_FLD_SERVICES[contains(PIN_FLD_SERVICE_OBJ,$serv-poid)]/PIN_FLD_ATTRIBUTES">
		<xsl:variable name="serv-attr">
  <!-- AmalinaRazif - 30072013 - To exclude USP Area Tagging in the bill display -->
        <xsl:for-each select="/invoice/TM_FLD_CUST_SRV_ATT_PROFILE/PIN_FLD_SERVICES[contains(PIN_FLD_SERVICE_OBJ,$serv-poid)]/PIN_FLD_ATTRIBUTES[(PIN_FLD_NAME != 'TM_USP' and PIN_FLD_NAME != 'INTERNATIONAL_CIRCUIT_NUM' and PIN_FLD_NAME != 'END_CUSTOMER_NAME' and PIN_FLD_NAME != 'CUSTOMER_PURCHASE_NO') and (PIN_FLD_VALUE != '') and (TM_FLD_BILL_DISPLAY != 'N')]/PIN_FLD_VALUE">
				<xsl:value-of select="."/>-</xsl:for-each>
		</xsl:variable>
      <!-- Trim last character for extra dash -->
		<SERVICE_ATTR>
			<xsl:value-of select="substring($serv-attr,1,string-length($serv-attr)-1)"/>
		</SERVICE_ATTR>
	</xsl:if>
	<!-- William Gunawan Adding For INTERNATIONAL_SERV_REF ,END_COMPANY_NAME ,PURCHASED_ORDER_NO M2R2 -->
	<xsl:if test = "/invoice/TM_FLD_CUST_SRV_ATT_PROFILE/PIN_FLD_SERVICES[contains(PIN_FLD_SERVICE_OBJ,$serv-poid)]/PIN_FLD_ATTRIBUTES[(PIN_FLD_NAME = 'INTERNATIONAL_CIRCUIT_NUM') and (TM_FLD_BILL_DISPLAY != 'N')]/PIN_FLD_VALUE != ''">
			<INTERNATIONAL_SERV><xsl:value-of select="/invoice/TM_FLD_CUST_SRV_ATT_PROFILE/PIN_FLD_SERVICES[contains(PIN_FLD_SERVICE_OBJ,$serv-poid)]/PIN_FLD_ATTRIBUTES[PIN_FLD_NAME = 'INTERNATIONAL_CIRCUIT_NUM']/PIN_FLD_VALUE"/></INTERNATIONAL_SERV>
	</xsl:if>
	<xsl:if test = "/invoice/TM_FLD_CUST_SRV_ATT_PROFILE/PIN_FLD_SERVICES[contains(PIN_FLD_SERVICE_OBJ,$serv-poid)]/PIN_FLD_ATTRIBUTES[(PIN_FLD_NAME = 'END_CUSTOMER_NAME') and (TM_FLD_BILL_DISPLAY != 'N')]/PIN_FLD_VALUE != ''">
			<END_COMPANY_NAME><xsl:value-of select="/invoice/TM_FLD_CUST_SRV_ATT_PROFILE/PIN_FLD_SERVICES[contains(PIN_FLD_SERVICE_OBJ,$serv-poid)]/PIN_FLD_ATTRIBUTES[PIN_FLD_NAME = 'END_CUSTOMER_NAME']/PIN_FLD_VALUE"/></END_COMPANY_NAME>
	</xsl:if>
	<xsl:if test = "/invoice/TM_FLD_CUST_SRV_ATT_PROFILE/PIN_FLD_SERVICES[contains(PIN_FLD_SERVICE_OBJ,$serv-poid)]/PIN_FLD_ATTRIBUTES[(PIN_FLD_NAME = 'CUSTOMER_PURCHASE_NO') and (TM_FLD_BILL_DISPLAY != 'N')]/PIN_FLD_VALUE != ''">
			<PURCHASED_ORDER_NO><xsl:value-of select="/invoice/TM_FLD_CUST_SRV_ATT_PROFILE/PIN_FLD_SERVICES[contains(PIN_FLD_SERVICE_OBJ,$serv-poid)]/PIN_FLD_ATTRIBUTES[PIN_FLD_NAME = 'CUSTOMER_PURCHASE_NO']/PIN_FLD_VALUE"/></PURCHASED_ORDER_NO>
	</xsl:if>
	<!-- William Gunawan End -->
	
  </xsl:template>
  <xsl:template name="event-template-Subscription-Charges">
    <xsl:param name="event-list"/>
    <xsl:if test="$event-list/PIN_FLD_EVENTS">
      <RC_CHARGES>
        <xsl:variable name="iptv_min_charge_count" select="count($event-list/PIN_FLD_EVENTS[contains(PIN_FLD_SYS_DESCR,'Minimum Charge')])"/>
        <xsl:if test="$iptv_min_charge_count &gt; 0">
          <xsl:variable name="iptv_charge_date" select="$event-list/PIN_FLD_EVENTS[contains(PIN_FLD_SYS_DESCR,'Minimum Charge')]/PIN_FLD_EARNED_END_T"/>
          <xsl:if test="count($event-list/PIN_FLD_EVENTS[contains(PIN_FLD_SYS_DESCR,'Minimum Charge')][not(contains(PIN_FLD_EARNED_END_T,$iptv_charge_date))]) &gt; 0">
            <RC_ITEM>
              <xsl:if test="contains($event-list/PIN_FLD_EVENTS/PIN_FLD_SYS_DESCR, ':')">
                <DESC>
                  <xsl:call-template name="substring-after-last">
                    <xsl:with-param name="string" select="$event-list/PIN_FLD_EVENTS[contains(PIN_FLD_SYS_DESCR,'Minimum Charge')][not (contains(PIN_FLD_EARNED_END_T,$iptv_charge_date))]/PIN_FLD_SYS_DESCR"/>
                    <xsl:with-param name="delimiter" select="': '"/>
                  </xsl:call-template>
                </DESC>
              </xsl:if>
              <xsl:if test="not(contains($event-list/PIN_FLD_EVENTS/PIN_FLD_SYS_DESCR, ':'))">
                <DESC>
                  <xsl:value-of select="PIN_FLD_SYS_DESCR"/>
                </DESC>
              </xsl:if>
              <AMT_GROSS>
                <xsl:value-of select="format-number(sum($event-list/PIN_FLD_EVENTS[contains(PIN_FLD_SYS_DESCR,'Minimum Charge')][not (contains(PIN_FLD_EARNED_END_T,$iptv_charge_date))]/PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id][PIN_FLD_IMPACT_TYPE != 128 and PIN_FLD_IMPACT_TYPE != 512]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
              </AMT_GROSS>
              <AMT_DISCOUNT>
                <xsl:value-of select="format-number(sum($event-list/PIN_FLD_EVENTS[contains(PIN_FLD_SYS_DESCR,'Minimum Charge')][not (contains(PIN_FLD_EARNED_END_T,$iptv_charge_date))]/PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id][PIN_FLD_IMPACT_TYPE = 128]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
              </AMT_DISCOUNT>
              <AMT_NETT>
                <xsl:value-of select="format-number(sum($event-list/PIN_FLD_EVENTS[contains(PIN_FLD_SYS_DESCR,'Minimum Charge')][not (contains(PIN_FLD_EARNED_END_T,$iptv_charge_date))]/PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
              </AMT_NETT>
              <ITEM_START_DATE>
                <xsl:call-template name="format-unix-date">
                  <xsl:with-param name="unix" select="$event-list/PIN_FLD_EVENTS[contains(PIN_FLD_SYS_DESCR,'Minimum Charge')][not(contains(PIN_FLD_EARNED_END_T,$iptv_charge_date))]/PIN_FLD_EARNED_START_T"/>
                </xsl:call-template>
              </ITEM_START_DATE>
              <ITEM_END_DATE>
                <xsl:call-template name="format-unix-date">
                  <xsl:with-param name="unix" select="$event-list/PIN_FLD_EVENTS[contains(PIN_FLD_SYS_DESCR,'Minimum Charge')][not(contains(PIN_FLD_EARNED_END_T,$iptv_charge_date))]/PIN_FLD_EARNED_END_T - 86400"/>
                </xsl:call-template>
              </ITEM_END_DATE>
            </RC_ITEM>
          </xsl:if>
          <RC_ITEM>
            <xsl:choose>
              <xsl:when test="contains($event-list/PIN_FLD_EVENTS/PIN_FLD_SYS_DESCR, ':')">
                <DESC>
                  <xsl:call-template name="substring-after-last">
                    <xsl:with-param name="string" select="$event-list/PIN_FLD_EVENTS[contains(PIN_FLD_SYS_DESCR,'Minimum Charge')][contains(PIN_FLD_EARNED_END_T,$iptv_charge_date)]/PIN_FLD_SYS_DESCR"/>
                    <xsl:with-param name="delimiter" select="': '"/>
                  </xsl:call-template>
                </DESC>
              </xsl:when>
              <xsl:otherwise>
                <DESC>
                  <xsl:value-of select="PIN_FLD_SYS_DESCR"/>
                </DESC>
              </xsl:otherwise>
            </xsl:choose>
            <AMT_GROSS>
              <xsl:value-of select="format-number(sum($event-list/PIN_FLD_EVENTS[contains(PIN_FLD_SYS_DESCR,'Minimum Charge')][contains(PIN_FLD_EARNED_END_T,$iptv_charge_date)]/PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id][PIN_FLD_IMPACT_TYPE != 128 and PIN_FLD_IMPACT_TYPE != 512]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
            </AMT_GROSS>
            <AMT_DISCOUNT>
              <xsl:value-of select="format-number(sum($event-list/PIN_FLD_EVENTS[contains(PIN_FLD_SYS_DESCR,'Minimum Charge')][contains(PIN_FLD_EARNED_END_T,$iptv_charge_date)]/PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id][PIN_FLD_IMPACT_TYPE = 128]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
            </AMT_DISCOUNT>
            <AMT_NETT>
              <xsl:value-of select="format-number(sum($event-list/PIN_FLD_EVENTS[contains(PIN_FLD_SYS_DESCR,'Minimum Charge')][contains(PIN_FLD_EARNED_END_T,$iptv_charge_date)]/PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
            </AMT_NETT>
            <ITEM_START_DATE>
              <xsl:call-template name="format-unix-date">
                <xsl:with-param name="unix" select="$event-list/PIN_FLD_EVENTS[contains(PIN_FLD_SYS_DESCR,'Minimum Charge')][contains(PIN_FLD_EARNED_END_T,$iptv_charge_date)]/PIN_FLD_EARNED_START_T"/>
              </xsl:call-template>
            </ITEM_START_DATE>
            <ITEM_END_DATE>
              <xsl:call-template name="format-unix-date">
                <xsl:with-param name="unix" select="$event-list/PIN_FLD_EVENTS[contains(PIN_FLD_SYS_DESCR,'Minimum Charge')][contains(PIN_FLD_EARNED_END_T,$iptv_charge_date)]/PIN_FLD_EARNED_END_T - 86400"/>
              </xsl:call-template>
            </ITEM_END_DATE>
          </RC_ITEM>
        </xsl:if>
        <xsl:for-each select="$event-list/PIN_FLD_EVENTS[not(contains(PIN_FLD_SYS_DESCR,'Minimum Charge'))]">
          <xsl:sort select="(*|*/*)[name()='PIN_FLD_EARNED_START_T']" order="ascending"/>
          <xsl:sort select="(*|*/*)[name()='PIN_FLD_SYS_DESCR']" order="ascending"/>
          <!--  <xsl:for-each select="PIN_FLD_EVENTS">  -->
          <!-- amer - 20110919 - remove items with 0 charge for global - start 03 -->
          <xsl:if test="not(contains(/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP,'GLOBAL') and format-number(PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT,'###,###,###,##0.00') = '0.00') ">
            <RC_ITEM>
              <xsl:if test="contains(PIN_FLD_SYS_DESCR, ':')">
                <DESC>
                  <xsl:call-template name="substring-after-last">
                    <xsl:with-param name="string" select="PIN_FLD_SYS_DESCR"/>
                    <xsl:with-param name="delimiter" select="': '"/>
                  </xsl:call-template>
                </DESC>
              </xsl:if>
              <xsl:if test="not(contains(PIN_FLD_SYS_DESCR, ':'))">
                <DESC>
                  <xsl:value-of select="PIN_FLD_SYS_DESCR"/>
                </DESC>
              </xsl:if>
              <xsl:choose>
                <xsl:when test="contains(/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP,'WHOLESALE') or contains(/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP,'GLOBAL')">
                  <AMT_GROSS/>
                  <AMT_DISCOUNT/>
                </xsl:when>
                <xsl:otherwise>
                  <AMT_GROSS>
                    <xsl:value-of select="format-number(sum(PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id][PIN_FLD_IMPACT_TYPE != 128 and PIN_FLD_IMPACT_TYPE != 512]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
                  </AMT_GROSS>
                  <AMT_DISCOUNT>
                    <xsl:value-of select="format-number(sum(PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id][PIN_FLD_IMPACT_TYPE = 128]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
                  </AMT_DISCOUNT>
                </xsl:otherwise>
              </xsl:choose>
              <AMT_NETT>
                <xsl:value-of select="format-number(PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT,'###,###,###,##0.00')"/>
              </AMT_NETT>
              <ITEM_START_DATE>
                <xsl:call-template name="format-unix-date">
                  <xsl:with-param name="unix" select="PIN_FLD_EARNED_START_T"/>
                </xsl:call-template>
              </ITEM_START_DATE>
              <ITEM_END_DATE>
                <xsl:call-template name="format-unix-date">
                  <xsl:with-param name="unix" select="PIN_FLD_EARNED_END_T - 86400"/>
                </xsl:call-template>
              </ITEM_END_DATE>
            </RC_ITEM>
          </xsl:if>
          <!-- amer - 20110919 - remove items with 0 charge for global - end 03 -->
          <!--  </xsl:for-each>  -->
        </xsl:for-each>
        <xsl:choose>
          <xsl:when test="contains(/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP,'WHOLESALE') or contains(/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP,'GLOBAL')">
            <AMT_GROSS_TOTAL/>
            <AMT_DISCOUNT_TOTAL/>
          </xsl:when>
          <xsl:otherwise>
            <AMT_GROSS_TOTAL>
              <xsl:value-of select="format-number(sum($event-list/PIN_FLD_EVENTS/PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id][PIN_FLD_IMPACT_TYPE != 128 and PIN_FLD_IMPACT_TYPE != 512]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
            </AMT_GROSS_TOTAL>
            <AMT_DISCOUNT_TOTAL>
              <xsl:value-of select="format-number(sum($event-list/PIN_FLD_EVENTS/PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id][PIN_FLD_IMPACT_TYPE = 128]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
            </AMT_DISCOUNT_TOTAL>
          </xsl:otherwise>
        </xsl:choose>
        <AMT_NETT_TOTAL>
		<xsl:choose>
		<xsl:when test="contains(format-number(sum($event-list/PIN_FLD_EVENTS/PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT),'###,###,###,##0.00'),'-0.00')">0.00</xsl:when>
		<xsl:otherwise>
				<xsl:value-of select="format-number(sum($event-list/PIN_FLD_EVENTS/PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT),'###,###,###,##0.00')" />
		</xsl:otherwise>
		</xsl:choose>
        </AMT_NETT_TOTAL>
      </RC_CHARGES>
    </xsl:if>
  </xsl:template>
  <xsl:template name="event-template-Non-Subscription-Charges">
    <xsl:param name="event-list"/>
    <xsl:if test="$event-list/PIN_FLD_EVENTS or contains($event-list/PIN_FLD_ITEM_OBJ, '/item/bill_discount')">
			  <xsl:variable name="cancelgrs"> <xsl:value-of select="sum($event-list/PIN_FLD_EVENTS[contains(PIN_FLD_EVENT_OBJ,'/event/billing/product/fee/cancel')]/PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT)" /></xsl:variable>
      <OTC_CHARGES>
        <xsl:for-each select="$event-list[not(contains(PIN_FLD_ITEM_OBJ, '/item/bill_discount'))]/PIN_FLD_EVENTS">
          <!-- amer - 20110919 - remove items with 0 charge for global - start 01 -->
          <xsl:if test="not(contains(/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP,'GLOBAL') and format-number(PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT,'###,###,###,##0.00') = '0.00') ">
            <OTC_ITEM>
              <xsl:if test="contains(PIN_FLD_SYS_DESCR, ':')">
                <DESC>
                  <xsl:call-template name="substring-after-last">
                    <xsl:with-param name="string" select="PIN_FLD_SYS_DESCR"/>
                    <xsl:with-param name="delimiter" select="': '"/>
                  </xsl:call-template>
                </DESC>
              </xsl:if>
              <xsl:if test="not(contains(PIN_FLD_SYS_DESCR, ':'))">
                <DESC>
                  <xsl:value-of select="PIN_FLD_SYS_DESCR"/>
                </DESC>
              </xsl:if>
              <xsl:choose>
                <xsl:when test="contains(/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP,'WHOLESALE') or contains(/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP,'GLOBAL')">
                  <AMT_GROSS/>
                  <AMT_DISCOUNT/>
                </xsl:when>
                <xsl:otherwise>
					<xsl:choose>
						<xsl:when test="contains(PIN_FLD_EVENT_OBJ,'/event/billing/product/fee/cancel')">
							<AMT_GROSS><xsl:value-of select="format-number(PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT,'###,###,###,##0.00')" /></AMT_GROSS>
						</xsl:when>
						<xsl:otherwise>
							<AMT_GROSS><xsl:value-of select="format-number(sum(PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id][PIN_FLD_IMPACT_TYPE != 128 and PIN_FLD_IMPACT_TYPE != 512]/PIN_FLD_AMOUNT),'###,###,###,##0.00')" /></AMT_GROSS>
						</xsl:otherwise>
					</xsl:choose>
                  <AMT_DISCOUNT>
                    <xsl:value-of select="format-number(sum(PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id][PIN_FLD_IMPACT_TYPE = 128]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
                  </AMT_DISCOUNT>
                </xsl:otherwise>
              </xsl:choose>
              <AMT_NETT>
                <xsl:value-of select="format-number(PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT,'###,###,###,##0.00')"/>
              </AMT_NETT>
              <ITEM_START_DATE>
                <xsl:call-template name="format-unix-date">
                  <xsl:with-param name="unix" select="PIN_FLD_EARNED_START_T"/>
                </xsl:call-template>
              </ITEM_START_DATE>
              <ITEM_END_DATE>
                <xsl:call-template name="format-unix-date">
                  <xsl:with-param name="unix" select="PIN_FLD_EARNED_END_T"/>
                </xsl:call-template>
              </ITEM_END_DATE>
            </OTC_ITEM>
          </xsl:if>
          <!-- amer - 20110919 - remove items with 0 charge for global - end 01 -->
        </xsl:for-each>
        <!-- amer 20140702 - include /item/bill_discount as otc start -->
        <xsl:if test="$event-list[contains(PIN_FLD_ITEM_OBJ, '/item/bill_discount')]">
        <xsl:for-each select="$event-list[contains(PIN_FLD_ITEM_OBJ, '/item/bill_discount')]">
      		<OTC_ITEM>
      		<DESC><xsl:value-of select="PIN_FLD_EVENTS/PIN_FLD_SYS_DESCR"/></DESC>
      		<AMT_GROSS>0.00</AMT_GROSS>
      		<AMT_DISCOUNT>
      		<xsl:value-of select="format-number(PIN_FLD_ITEM_TOTAL,'###,###,###,##0.00')"/>
      		</AMT_DISCOUNT>
      		<AMT_NETT>
      		<xsl:value-of select="format-number(PIN_FLD_ITEM_TOTAL,'###,###,###,##0.00')"/>
      		</AMT_NETT>
          <ITEM_START_DATE>
            <xsl:call-template name="format-unix-date">
              <xsl:with-param name="unix" select="PIN_FLD_EVENTS/PIN_FLD_EARNED_START_T"/>
            </xsl:call-template>
          </ITEM_START_DATE>
          <ITEM_END_DATE>
            <xsl:call-template name="format-unix-date">
              <xsl:with-param name="unix" select="PIN_FLD_EVENTS/PIN_FLD_EARNED_END_T"/>
            </xsl:call-template>
          </ITEM_END_DATE>
         </OTC_ITEM>
        </xsl:for-each>
        </xsl:if>
        <!-- amer 20140702 - include /item/bill_discount as otc end -->
        <xsl:choose>
          <xsl:when test="contains(/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP,'WHOLESALE') or contains(/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP,'GLOBAL')">
            <AMT_GROSS_TOTAL/>
            <xsl:choose>
              <xsl:when test="contains($event-list/PIN_FLD_ITEM_OBJ, '/item/bill_discount')">
                <AMT_DISCOUNT_TOTAL><xsl:value-of select="format-number(sum($event-list[contains(PIN_FLD_ITEM_OBJ, '/item/bill_discount')]/PIN_FLD_ITEM_TOTAL),'###,###,###,##0.00')"/></AMT_DISCOUNT_TOTAL>
              </xsl:when>
              <xsl:otherwise>
                <AMT_DISCOUNT_TOTAL/>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:when>
          <xsl:otherwise>
            <AMT_GROSS_TOTAL>
              <xsl:value-of select="format-number(sum($event-list/PIN_FLD_EVENTS/PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id][PIN_FLD_IMPACT_TYPE != 128 and PIN_FLD_IMPACT_TYPE != 512]/PIN_FLD_AMOUNT) ,'###,###,###,##0.00')"/>
            </AMT_GROSS_TOTAL>
            <AMT_DISCOUNT_TOTAL>
              <xsl:value-of select="format-number(sum($event-list/PIN_FLD_EVENTS/PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id][PIN_FLD_IMPACT_TYPE = 128]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
            </AMT_DISCOUNT_TOTAL>
          </xsl:otherwise>
        </xsl:choose>
        <AMT_NETT_TOTAL>
		<xsl:choose>
		<xsl:when test="contains(format-number(sum($event-list/PIN_FLD_EVENTS/PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT) + sum($event-list[contains(PIN_FLD_ITEM_OBJ, '/item/bill_discount')]/PIN_FLD_ITEM_TOTAL),'###,###,###,##0.00'),'-0.00')">0.00</xsl:when>
		<xsl:otherwise>
				<xsl:value-of select="format-number(sum($event-list/PIN_FLD_EVENTS/PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT),'###,###,###,##0.00')" />
		</xsl:otherwise>
		</xsl:choose>
        </AMT_NETT_TOTAL>
      </OTC_CHARGES>
    </xsl:if>
  </xsl:template>
  <!-- amer 20130902 burstable -->
  <xsl:template name="event-template-Usage-Charges-burstable">
    <xsl:param name="event-list"/>
    <USG_CHARGES>
    <USG_ITEM>
      <USG_SECTION>
        <xsl:call-template name="usage-Heading">
          <xsl:with-param name="usageSection" select="$event-list/PIN_FLD_USAGE_CLASS"/>
        </xsl:call-template>
      </USG_SECTION>
      <USG_SUB_SECTION/>
      <xsl:for-each select="$event-list">
        <USG_SUB_ITEM>
          <USG_CATEGORY>Burstable</USG_CATEGORY>
          <ITEM_START_DATE>
            <xsl:call-template name="format-usage-date">
              <xsl:with-param name="date" select="PIN_FLD_CALL_DATE"/>
            </xsl:call-template>
          </ITEM_START_DATE>
          <ITEM_END_DATE>
            <xsl:call-template name="format-unix-date-tm-reward">
              <xsl:with-param name="unix" select="PIN_FLD_END_T"/>
            </xsl:call-template>
          </ITEM_END_DATE>
          <A_NUMBER>
            <xsl:value-of select="PIN_FLD_CALLING_NUMBER"/>
          </A_NUMBER>
          <B_NUMBER/>
          <B_LOCATION/>
          <DURATION/>
          <AMT_GROSS/>
          <AMT_DISCOUNT/>
          <AMT_NETT>
            <xsl:if test="PIN_FLD_TOTAL[@elem = $primary-currency-id]">
              <xsl:value-of select="format-number(PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT,'###,###,###,##0.00')"/>
            </xsl:if>
          </AMT_NETT>
        </USG_SUB_ITEM>
      </xsl:for-each>
      <AMT_TOTAL_GROSS/>
      <AMT_TOTAL_DISCOUNT/>
      <AMT_TOTAL_NETT>
        <xsl:value-of select="format-number(sum($event-list/PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
      </AMT_TOTAL_NETT>
    </USG_ITEM>
    </USG_CHARGES>
  </xsl:template>
  <xsl:template name="event-template-Usage-Charges">
    <xsl:param name="event-list"/>
    <xsl:if test="$event-list/PIN_FLD_EVENTS">
      <xsl:if test="not($event-list/PIN_FLD_EVENTS/PIN_FLD_BAL_IMPACTS[contains(PIN_FLD_IMPACT_TYPE,'512')])">
        <USG_CHARGES>
          <!--  National to fix  -->
          <xsl:call-template name="event-template-Usage-Detail">
            <xsl:with-param name="event-list" select="$event-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='NL01' and sum(PIN_FLD_BAL_IMPACTS[PIN_FLD_IMPACT_TYPE != 128 and PIN_FLD_IMPACT_TYPE != 512]/PIN_FLD_AMOUNT) >0]"/>
            <xsl:with-param name="usg-section">NL01</xsl:with-param>
          </xsl:call-template>
          <!--  National to mobile  -->
          <xsl:call-template name="event-template-Usage-Detail">
            <xsl:with-param name="event-list" select="$event-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='NL02']"/>
            <xsl:with-param name="usg-section">NL02</xsl:with-param>
          </xsl:call-template>
          <!--  International to fix  -->
          <xsl:call-template name="event-template-Usage-Detail">
            <xsl:with-param name="event-list" select="$event-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='IL01']"/>
            <xsl:with-param name="usg-section">IL01</xsl:with-param>
          </xsl:call-template>
          <!--  International to mobile  -->
          <xsl:call-template name="event-template-Usage-Detail">
            <xsl:with-param name="event-list" select="$event-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='IL02']"/>
            <xsl:with-param name="usg-section">IL02</xsl:with-param>
          </xsl:call-template>
          <!--  National operator  -->
          <xsl:call-template name="event-template-Usage-Detail">
            <xsl:with-param name="event-list" select="$event-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='NA01']"/>
            <xsl:with-param name="usg-section">NA01</xsl:with-param>
          </xsl:call-template>
          <xsl:call-template name="event-template-Usage-Detail">
            <xsl:with-param name="event-list" select="$event-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='NA02']"/>
            <xsl:with-param name="usg-section">NA02</xsl:with-param>
          </xsl:call-template>
          <xsl:call-template name="event-template-Usage-Detail">
            <xsl:with-param name="event-list" select="$event-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='NA03']"/>
            <xsl:with-param name="usg-section">NA03</xsl:with-param>
          </xsl:call-template>
          <xsl:call-template name="event-template-Usage-Detail">
            <xsl:with-param name="event-list" select="$event-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='NA04']"/>
            <xsl:with-param name="usg-section">NA04</xsl:with-param>
          </xsl:call-template>
          <xsl:call-template name="event-template-Usage-Detail">
            <xsl:with-param name="event-list" select="$event-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='NA05']"/>
            <xsl:with-param name="usg-section">NA05</xsl:with-param>
          </xsl:call-template>
          <!--  International operator  -->
          <xsl:call-template name="event-template-Usage-Detail">
            <xsl:with-param name="event-list" select="$event-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='IT01']"/>
            <xsl:with-param name="usg-section">IT01</xsl:with-param>
          </xsl:call-template>
          <!--  National to mobile  -->
          <xsl:call-template name="event-template-Usage-Detail">
            <xsl:with-param name="event-list" select="$event-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='IT02']"/>
            <xsl:with-param name="usg-section">IT02</xsl:with-param>
          </xsl:call-template>
          <!--  International to fix  -->
          <xsl:call-template name="event-template-Usage-Detail">
            <xsl:with-param name="event-list" select="$event-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='IT03']"/>
            <xsl:with-param name="usg-section">IT03</xsl:with-param>
          </xsl:call-template>
          <!--   Special Numbers / Others   -->
          <xsl:call-template name="event-template-Usage-Special">
            <xsl:with-param name="event-list" select="$event-list/PIN_FLD_EVENTS[contains(PIN_FLD_USAGE_CLASS,'OS')]"/>
            <xsl:with-param name="usg-section">OS</xsl:with-param>
          </xsl:call-template>
          <xsl:call-template name="event-template-Usage-Detail">
            <xsl:with-param name="event-list" select="$event-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='SOD']"/>
            <xsl:with-param name="usg-section">SOD</xsl:with-param>
          </xsl:call-template>
          <!--  IPTV  -->
          <xsl:call-template name="event-template-Usage-IPTV">
            <xsl:with-param name="event-list" select="$event-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='VD00']"/>
            <xsl:with-param name="usg-section">VOD</xsl:with-param>
          </xsl:call-template>
          <xsl:call-template name="event-template-Usage-Detail">
            <xsl:with-param name="event-list" select="$event-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='VOD']"/>
            <xsl:with-param name="usg-section">VOD</xsl:with-param>
          </xsl:call-template>
          <xsl:call-template name="event-template-Usage-IPTV">
            <xsl:with-param name="event-list" select="$event-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='SD00']"/>
            <xsl:with-param name="usg-section">SOD</xsl:with-param>
          </xsl:call-template>
          <!-- Interactive IOD -->
          <xsl:call-template name="event-template-Usage-IPTV">
            <xsl:with-param name="event-list" select="$event-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='ID00']"/>
            <xsl:with-param name="usg-section">ID00</xsl:with-param>
          </xsl:call-template>
          <xsl:call-template name="event-template-Usage-InfoBlast">
            <xsl:with-param name="event-list" select="$event-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='IB00']"/>
            <xsl:with-param name="usg-section">IB00</xsl:with-param>
          </xsl:call-template>
		  <!--  William adding Burstable Usage M2R2 20140421 start--> 
			  <xsl:call-template name="event-template-Usage-Burstable">
			  <xsl:with-param name="event-list" select="$event-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='OC01']" />
			  <xsl:with-param name="usg-section">OC01</xsl:with-param>
			  </xsl:call-template>
		  <!--  William adding Burstable Usage M2R2 20140421 end--> 
        </USG_CHARGES>
      </xsl:if>
    </xsl:if>
  </xsl:template>
  
  <!-- William 06022014:  NEW TEMPLATE FOR Burstable Usage -->
<xsl:template name="event-template-Usage-Burstable">
<xsl:param name="event-list" />
<xsl:param name="usg-section" />
	<xsl:if test ="$event-list">
		<USG_ITEM>
			<USG_SECTION><xsl:call-template name="usage-Heading"><xsl:with-param name="usageSection" select="$usg-section" /></xsl:call-template></USG_SECTION>
			<USG_SUB_SECTION><xsl:call-template name="usage-Section"><xsl:with-param name="usageSection" select="$usg-section" /></xsl:call-template></USG_SUB_SECTION>
			<xsl:for-each select="$event-list">
			<USG_SUB_ITEM>
				<USG_CATEGORY>Burstable</USG_CATEGORY>
				<ITEM_START_DATE><xsl:call-template name="format-unix-date"><xsl:with-param name="unix" select="PIN_FLD_EARNED_START_T" /></xsl:call-template></ITEM_START_DATE>
				<ITEM_END_DATE><xsl:call-template name="format-unix-date"><xsl:with-param name="unix" select="PIN_FLD_EARNED_END_T" /></xsl:call-template></ITEM_END_DATE>
				<A_NUMBER/>
				<B_NUMBER/>
				<B_LOCATION><xsl:value-of select="../PIN_FLD_LOGIN"/></B_LOCATION>
				<QUANTITY><xsl:if test="PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id]"><xsl:value-of select="format-number(sum(PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id][PIN_FLD_IMPACT_TYPE != 128 and PIN_FLD_IMPACT_TYPE != 512]/PIN_FLD_QUANTITY),'###,###,###,##0')" /></xsl:if></QUANTITY>
				<AMT_GROSS/>
				<AMT_DISCOUNT/>
				<AMT_NETT><xsl:if test="PIN_FLD_TOTAL[@elem = $primary-currency-id]"><xsl:value-of select="format-number(PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT,'###,###,###,##0.00')" /></xsl:if></AMT_NETT>
			</USG_SUB_ITEM>
			</xsl:for-each>
			<AMT_TOTAL_GROSS/>
			<AMT_TOTAL_DISCOUNT/>
			<AMT_TOTAL_NETT><xsl:value-of select="format-number(sum($event-list/PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/></AMT_TOTAL_NETT>
		</USG_ITEM>
	</xsl:if>
</xsl:template>
<!-- William 06022014:  NEW TEMPLATE FOR Burstable Usage End-->

  
  <xsl:template name="event-template-Usage-Detail">
    <xsl:param name="event-list"/>
    <xsl:param name="usg-section"/>
    <xsl:if test="$event-list">
      <USG_ITEM>
        <!--   <USG_SERVICE><xsl:value-of select="$event-list/PIN_FLD_ITEM_OBJ" /></USG_SERVICE>   -->
        <USG_SECTION>
          <xsl:call-template name="usage-Heading">
            <xsl:with-param name="usageSection" select="$usg-section"/>
          </xsl:call-template>
        </USG_SECTION>
        <USG_SUB_SECTION>
          <xsl:call-template name="usage-Section">
            <xsl:with-param name="usageSection" select="$usg-section"/>
          </xsl:call-template>
        </USG_SUB_SECTION>
        <xsl:for-each select="$event-list">
          <USG_SUB_ITEM>
            <USG_CATEGORY>
</USG_CATEGORY>
            <ITEM_START_DATE>
              <xsl:call-template name="format-usage-date">
                <xsl:with-param name="date" select="PIN_FLD_CALL_DATE"/>
              </xsl:call-template>
            </ITEM_START_DATE>
            <ITEM_START_TIME>
              <xsl:call-template name="format-usage-time">
                <xsl:with-param name="date" select="PIN_FLD_CALL_DATE"/>
              </xsl:call-template>
            </ITEM_START_TIME>
            <A_NUMBER>
              <xsl:value-of select="PIN_FLD_CALLING_NUMBER"/>
            </A_NUMBER>
            <B_NUMBER>
              <xsl:value-of select="PIN_FLD_CALLED_NUMBER"/>
            </B_NUMBER>
            <B_LOCATION>
              <xsl:value-of select="PIN_FLD_DESCR"/>
            </B_LOCATION>
            <DURATION>
              <xsl:call-template name="format-duration">
                <xsl:with-param name="seconds" select="PIN_FLD_NET_QUANTITY"/>
              </xsl:call-template>
            </DURATION>
            <xsl:choose>
              <xsl:when test="contains(/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP,'WHOLESALE') or contains(/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP,'GLOBAL')">
                <AMT_GROSS/>
                <AMT_DISCOUNT/>
              </xsl:when>
              <xsl:otherwise>
                <AMT_GROSS>
                  <xsl:if test="PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = 1000107]">
                    <xsl:value-of select="format-number(sum(PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = 1000107][PIN_FLD_IMPACT_TYPE != 128 and PIN_FLD_IMPACT_TYPE != 512]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
                  </xsl:if>
                  <xsl:if test="PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id]">
                    <xsl:value-of select="format-number(sum(PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id][PIN_FLD_IMPACT_TYPE != 128 and PIN_FLD_IMPACT_TYPE != 512]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
                  </xsl:if>
                </AMT_GROSS>
                <AMT_DISCOUNT>
                  <xsl:value-of select="format-number(sum(PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id][PIN_FLD_IMPACT_TYPE = 128]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
                </AMT_DISCOUNT>
              </xsl:otherwise>
            </xsl:choose>
            <AMT_NETT>
              <xsl:if test="PIN_FLD_TOTAL[@elem = 1000107]">
                <xsl:value-of select="format-number(PIN_FLD_TOTAL[@elem  = 1000107]/PIN_FLD_AMOUNT,'###,###,###,##0.00')"/>
              </xsl:if>
              <xsl:if test="PIN_FLD_TOTAL[@elem = $primary-currency-id]">
                <xsl:value-of select="format-number(PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT,'###,###,###,##0.00')"/>
              </xsl:if>
            </AMT_NETT>
          </USG_SUB_ITEM>
        </xsl:for-each>
        <xsl:choose>
          <xsl:when test="contains(/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP,'WHOLESALE') or contains(/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP,'GLOBAL')">
            <AMT_TOTAL_GROSS/>
            <AMT_TOTAL_DISCOUNT/>
          </xsl:when>
          <xsl:otherwise>
            <AMT_TOTAL_GROSS>
              <xsl:value-of select="format-number(sum($event-list/PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id][PIN_FLD_IMPACT_TYPE != 128 and PIN_FLD_IMPACT_TYPE != 512]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
            </AMT_TOTAL_GROSS>
            <AMT_TOTAL_DISCOUNT>
              <xsl:value-of select="format-number(sum($event-list/PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id][PIN_FLD_IMPACT_TYPE = 128]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
            </AMT_TOTAL_DISCOUNT>
          </xsl:otherwise>
        </xsl:choose>
        <AMT_TOTAL_NETT>
          <xsl:value-of select="format-number(sum($event-list/PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
        </AMT_TOTAL_NETT>
      </USG_ITEM>
    </xsl:if>
  </xsl:template>
  <xsl:template name="event-template-Usage-InfoBlast">
    <xsl:param name="event-list"/>
    <xsl:param name="usg-section"/>
    <xsl:if test="$event-list">
      <USG_ITEM>
        <!--   <USG_SERVICE><xsl:value-of select="$event-list/PIN_FLD_ITEM_OBJ" /></USG_SERVICE>   -->
        <USG_SECTION>
          <xsl:call-template name="usage-Heading">
            <xsl:with-param name="usageSection" select="$usg-section"/>
          </xsl:call-template>
        </USG_SECTION>
        <USG_SUB_SECTION>
          <xsl:call-template name="usage-Section-detail">
            <xsl:with-param name="usageSection" select="$usg-section"/>
          </xsl:call-template>
        </USG_SUB_SECTION>
        <xsl:for-each select="$event-list">
          <USG_SUB_ITEM>
            <USG_CATEGORY/>
            <ITEM_START_DATE/>
            <ITEM_START_TIME/>
            <B_NUMBER/>
            <B_LOCATION/>
            <DURATION/>
            <AMT_GROSS/>
            <AMT_DISCOUNT/>
            <AMT_NETT>
              <xsl:if test="PIN_FLD_TOTAL[@elem = 1000107]">
                <xsl:value-of select="format-number(PIN_FLD_TOTAL[@elem  = 1000107]/PIN_FLD_AMOUNT,'###,###,###,##0.00')"/>
              </xsl:if>
              <xsl:if test="PIN_FLD_TOTAL[@elem = $primary-currency-id]">
                <xsl:value-of select="format-number(PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT,'###,###,###,##0.00')"/>
              </xsl:if>
            </AMT_NETT>
          </USG_SUB_ITEM>
        </xsl:for-each>
        <xsl:choose>
          <xsl:when test="contains(/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP,'WHOLESALE') or contains(/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP,'GLOBAL')">
            <AMT_TOTAL_GROSS/>
            <AMT_TOTAL_DISCOUNT/>
          </xsl:when>
          <xsl:otherwise>
            <AMT_TOTAL_GROSS>
              <xsl:value-of select="format-number(sum($event-list/PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = 1000107][PIN_FLD_IMPACT_TYPE != 128 and PIN_FLD_IMPACT_TYPE != 512]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
            </AMT_TOTAL_GROSS>
            <AMT_TOTAL_DISCOUNT>
              <xsl:value-of select="format-number(sum($event-list/PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = 1000107][PIN_FLD_IMPACT_TYPE = 128]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
            </AMT_TOTAL_DISCOUNT>
          </xsl:otherwise>
        </xsl:choose>
        <AMT_TOTAL_NETT>
          <xsl:value-of select="format-number(sum($event-list/PIN_FLD_TOTAL[@elem  = 1000107]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
        </AMT_TOTAL_NETT>
      </USG_ITEM>
    </xsl:if>
  </xsl:template>
  <xsl:template name="event-template-Usage-Special">
    <xsl:param name="event-list"/>
    <xsl:param name="usg-section"/>
    <xsl:if test="$event-list">
      <USG_ITEM>
        <USG_SECTION>
          <xsl:call-template name="usage-Heading">
            <xsl:with-param name="usageSection" select="$usg-section"/>
          </xsl:call-template>
        </USG_SECTION>
        <USG_SUB_SECTION>
          <xsl:call-template name="usage-Section">
            <xsl:with-param name="usageSection" select="$usg-section"/>
          </xsl:call-template>
        </USG_SUB_SECTION>
        <xsl:for-each select="$event-list">
          <USG_SUB_ITEM>
            <USG_CATEGORY>
              <xsl:call-template name="usage-Category">
                <xsl:with-param name="usageCategory">
                  <xsl:value-of select="PIN_FLD_USAGE_CLASS"/>
                </xsl:with-param>
              </xsl:call-template>
            </USG_CATEGORY>
            <ITEM_START_DATE>
              <xsl:call-template name="format-usage-date">
                <xsl:with-param name="date" select="PIN_FLD_CALL_DATE"/>
              </xsl:call-template>
            </ITEM_START_DATE>
            <ITEM_START_TIME>
              <xsl:call-template name="format-usage-time">
                <xsl:with-param name="date" select="PIN_FLD_CALL_DATE"/>
              </xsl:call-template>
            </ITEM_START_TIME>
            <A_NUMBER>
              <xsl:value-of select="PIN_FLD_CALLING_NUMBER"/>
            </A_NUMBER>
            <B_NUMBER>
              <xsl:value-of select="PIN_FLD_CALLED_NUMBER"/>
            </B_NUMBER>
            <B_LOCATION>
              <!-- Added by David 20100520 to send the matrix transformation value to BPM as they are using this field instead of USG_CATEGORY above -->
              <xsl:call-template name="usage-Category">
                <xsl:with-param name="usageCategory">
                  <xsl:value-of select="PIN_FLD_USAGE_CLASS"/>
                </xsl:with-param>
              </xsl:call-template>
            </B_LOCATION>
            <DURATION>
              <xsl:call-template name="format-duration">
                <xsl:with-param name="seconds" select="PIN_FLD_NET_QUANTITY"/>
              </xsl:call-template>
            </DURATION>
            <xsl:choose>
              <xsl:when test="contains(/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP,'WHOLESALE') or contains(/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP,'GLOBAL')">
                <AMT_GROSS/>
                <AMT_DISCOUNT/>
              </xsl:when>
              <xsl:otherwise>
                <AMT_GROSS>
                  <xsl:value-of select="format-number(sum(PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id][PIN_FLD_IMPACT_TYPE != 128 and PIN_FLD_IMPACT_TYPE != 512]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
                </AMT_GROSS>
                <AMT_DISCOUNT>
                  <xsl:value-of select="format-number(sum(PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id][PIN_FLD_IMPACT_TYPE = 128]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
                </AMT_DISCOUNT>
              </xsl:otherwise>
            </xsl:choose>
            <AMT_NETT>
              <xsl:value-of select="format-number(PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT,'###,###,###,##0.00')"/>
            </AMT_NETT>
          </USG_SUB_ITEM>
        </xsl:for-each>
        <xsl:choose>
          <xsl:when test="contains(/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP,'WHOLESALE') or contains(/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP,'GLOBAL')">
            <AMT_TOTAL_GROSS/>
            <AMT_TOTAL_DISCOUNT/>
          </xsl:when>
          <xsl:otherwise>
            <AMT_TOTAL_GROSS>
              <xsl:value-of select="format-number(sum($event-list/PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id][PIN_FLD_IMPACT_TYPE != 128 and PIN_FLD_IMPACT_TYPE != 512]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
            </AMT_TOTAL_GROSS>
            <AMT_TOTAL_DISCOUNT>
              <xsl:value-of select="format-number(sum($event-list/PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id][PIN_FLD_IMPACT_TYPE = 128]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
            </AMT_TOTAL_DISCOUNT>
          </xsl:otherwise>
        </xsl:choose>
        <AMT_TOTAL_NETT>
          <xsl:value-of select="format-number(sum($event-list/PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
        </AMT_TOTAL_NETT>
      </USG_ITEM>
    </xsl:if>
  </xsl:template>
  <xsl:template name="event-template-Usage-IPTV">
    <xsl:param name="event-list"/>
    <xsl:param name="usg-section"/>
    <xsl:if test="$event-list">
      <USG_ITEM>
        <USG_SECTION>
          <xsl:call-template name="usage-Heading">
            <xsl:with-param name="usageSection" select="$usg-section"/>
          </xsl:call-template>
        </USG_SECTION>
        <!--<USG_SUB_SECTION><xsl:call-template name="usage-Section"><xsl:with-param name="usageSection" select="$usg-section" /></xsl:call-template></USG_SUB_SECTION>-->
        <USG_SUB_SECTION>
</USG_SUB_SECTION>
        <xsl:for-each select="$event-list">
          <USG_SUB_ITEM>
            <USG_CATEGORY>
              <!-- <xsl:call-template name="usage-Category"> -->
              <!-- <xsl:with-param name="usageCategory"> -->
              <xsl:value-of select="PIN_FLD_USAGE_CLASS"/>
              <!-- </xsl:with-param></xsl:call-template> -->
            </USG_CATEGORY>
            <ITEM_START_DATE>
              <xsl:call-template name="format-usage-date">
                <xsl:with-param name="date" select="PIN_FLD_CALL_DATE"/>
              </xsl:call-template>
            </ITEM_START_DATE>
            <ITEM_START_TIME>
              <xsl:call-template name="format-usage-time">
                <xsl:with-param name="date" select="PIN_FLD_CALL_DATE"/>
              </xsl:call-template>
            </ITEM_START_TIME>
            <B_NUMBER>
              <xsl:value-of select="PIN_FLD_CALLED_NUMBER"/>
            </B_NUMBER>
            <B_LOCATION>
              <xsl:value-of select="PIN_FLD_DESCR"/>
            </B_LOCATION>
            <DURATION>
              <xsl:call-template name="format-duration">
                <xsl:with-param name="seconds" select="PIN_FLD_NET_QUANTITY"/>
              </xsl:call-template>
            </DURATION>
            <AMT_GROSS>
              <xsl:value-of select="format-number(PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT,'###,###,###,##0.00')"/>
            </AMT_GROSS>
            <AMT_DISCOUNT>
              <xsl:value-of select="format-number(sum(PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id][PIN_FLD_IMPACT_TYPE = 128]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
            </AMT_DISCOUNT>
            <AMT_NETT>
              <xsl:value-of select="format-number(PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT,'###,###,###,##0.00')"/>
            </AMT_NETT>
          </USG_SUB_ITEM>
        </xsl:for-each>
        <xsl:choose>
          <xsl:when test="contains(/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP,'WHOLESALE') or contains(/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP,'GLOBAL')">
            <AMT_TOTAL_GROSS/>
            <AMT_TOTAL_DISCOUNT/>
          </xsl:when>
          <xsl:otherwise>
            <AMT_TOTAL_GROSS>
              <xsl:value-of select="format-number(sum($event-list/PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id][PIN_FLD_IMPACT_TYPE != 128 and PIN_FLD_IMPACT_TYPE != 512]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
            </AMT_TOTAL_GROSS>
            <AMT_TOTAL_DISCOUNT>
              <xsl:value-of select="format-number(sum($event-list/PIN_FLD_BAL_IMPACTS[PIN_FLD_RESOURCE_ID  = $primary-currency-id][PIN_FLD_IMPACT_TYPE = 128]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
            </AMT_TOTAL_DISCOUNT>
          </xsl:otherwise>
        </xsl:choose>
        <AMT_TOTAL_NETT>
          <xsl:value-of select="format-number(sum($event-list/PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
        </AMT_TOTAL_NETT>
      </USG_ITEM>
    </xsl:if>
  </xsl:template>
  <xsl:template name="event-template-Usage">
    <xsl:param name="item-list"/>
    <!--  National to fix  -->
    <xsl:call-template name="event-template-Usage-Sum">
      <xsl:with-param name="event-list" select="$item-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='NL01' and sum(PIN_FLD_BAL_IMPACTS[PIN_FLD_IMPACT_TYPE != 128 and PIN_FLD_IMPACT_TYPE != 512]/PIN_FLD_AMOUNT) >0]"/>
      <xsl:with-param name="usg-section">NL01</xsl:with-param>
    </xsl:call-template>
    <!--  National to mobile  -->
    <xsl:call-template name="event-template-Usage-Sum">
      <xsl:with-param name="event-list" select="$item-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='NL02']"/>
      <xsl:with-param name="usg-section">NL02</xsl:with-param>
    </xsl:call-template>
    <!--  International to fix  -->
    <xsl:call-template name="event-template-Usage-Sum">
      <xsl:with-param name="event-list" select="$item-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='IL01']"/>
      <xsl:with-param name="usg-section">IL01</xsl:with-param>
    </xsl:call-template>
    <!--  International to mobile  -->
    <xsl:call-template name="event-template-Usage-Sum">
      <xsl:with-param name="event-list" select="$item-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='IL02']"/>
      <xsl:with-param name="usg-section">IL02</xsl:with-param>
    </xsl:call-template>
    <!--  National operator  -->
    <xsl:call-template name="event-template-Usage-Sum">
      <xsl:with-param name="event-list" select="$item-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='NA01']"/>
      <xsl:with-param name="usg-section">NA01</xsl:with-param>
    </xsl:call-template>
    <xsl:call-template name="event-template-Usage-Sum">
      <xsl:with-param name="event-list" select="$item-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='NA02']"/>
      <xsl:with-param name="usg-section">NA02</xsl:with-param>
    </xsl:call-template>
    <xsl:call-template name="event-template-Usage-Sum">
      <xsl:with-param name="event-list" select="$item-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='NA03']"/>
      <xsl:with-param name="usg-section">NA03</xsl:with-param>
    </xsl:call-template>
    <xsl:call-template name="event-template-Usage-Sum">
      <xsl:with-param name="event-list" select="$item-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='NA04']"/>
      <xsl:with-param name="usg-section">NA04</xsl:with-param>
    </xsl:call-template>
    <xsl:call-template name="event-template-Usage-Sum">
      <xsl:with-param name="event-list" select="$item-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='NA05']"/>
      <xsl:with-param name="usg-section">NA05</xsl:with-param>
    </xsl:call-template>
    <!--  International operator  -->
    <xsl:call-template name="event-template-Usage-Sum">
      <xsl:with-param name="event-list" select="$item-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='IT01']"/>
      <xsl:with-param name="usg-section">IT01</xsl:with-param>
    </xsl:call-template>
    <!--  National to mobile  -->
    <xsl:call-template name="event-template-Usage-Sum">
      <xsl:with-param name="event-list" select="$item-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='IT02']"/>
      <xsl:with-param name="usg-section">IT02</xsl:with-param>
    </xsl:call-template>
    <!--  International to fix  -->
    <xsl:call-template name="event-template-Usage-Sum">
      <xsl:with-param name="event-list" select="$item-list/PIN_FLD_EVENTS[PIN_FLD_USAGE_CLASS='IT03']"/>
      <xsl:with-param name="usg-section">IT03</xsl:with-param>
    </xsl:call-template>
    <!--   Special Numbers / Others   -->
    <xsl:call-template name="event-template-Usage-Sum">
      <xsl:with-param name="event-list" select="$item-list/PIN_FLD_EVENTS[contains(PIN_FLD_USAGE_CLASS,'OS')]"/>
      <xsl:with-param name="usg-section">OS</xsl:with-param>
    </xsl:call-template>
    <xsl:call-template name="event-template-Usage-Sum">
      <xsl:with-param name="event-list" select="$item-list/PIN_FLD_EVENTS[contains(PIN_FLD_USAGE_CLASS,'SOD')]"/>
      <xsl:with-param name="usg-section">SOD</xsl:with-param>
    </xsl:call-template>
    <xsl:call-template name="event-template-Usage-Sum">
      <xsl:with-param name="event-list" select="$item-list/PIN_FLD_EVENTS[contains(PIN_FLD_USAGE_CLASS,'VOD')]"/>
      <xsl:with-param name="usg-section">VOD</xsl:with-param>
    </xsl:call-template>
    <xsl:call-template name="event-template-Usage-Sum">
      <xsl:with-param name="event-list" select="$item-list/PIN_FLD_EVENTS[contains(PIN_FLD_USAGE_CLASS,'IB00')]"/>
      <xsl:with-param name="usg-section">IB00</xsl:with-param>
    </xsl:call-template>
    <xsl:call-template name="event-template-Usage-Sum">
      <xsl:with-param name="event-list" select="$item-list/PIN_FLD_EVENTS[contains(PIN_FLD_USAGE_CLASS,'VD00')]"/>
      <xsl:with-param name="usg-section">VOD</xsl:with-param>
    </xsl:call-template>
    <xsl:call-template name="event-template-Usage-Sum">
      <xsl:with-param name="event-list" select="$item-list/PIN_FLD_EVENTS[contains(PIN_FLD_USAGE_CLASS,'SD00')]"/>
      <xsl:with-param name="usg-section">SOD</xsl:with-param>
    </xsl:call-template>
    <xsl:call-template name="event-template-Usage-Sum">
      <xsl:with-param name="event-list" select="$item-list/PIN_FLD_EVENTS[contains(PIN_FLD_USAGE_CLASS,'ID00')]"/>
      <xsl:with-param name="usg-section">ID00</xsl:with-param>
    </xsl:call-template>
    <xsl:call-template name="event-template-Usage-Sum-burst">
      <xsl:with-param name="event-list" select="$item-list/PIN_FLD_EVENTS[contains(PIN_FLD_USAGE_CLASS,'WB00')]"/>
      <xsl:with-param name="usg-section">WB00</xsl:with-param>
	  <!-- William 20140521 usg summary for burstable M2R2 Start-->
    </xsl:call-template>
	 <xsl:call-template name="event-template-Usage-Sum-burstable">
      <xsl:with-param name="event-list" select="$item-list/PIN_FLD_EVENTS[contains(PIN_FLD_USAGE_CLASS,'OC01')]"/>
      <xsl:with-param name="usg-section">OC01</xsl:with-param>
    </xsl:call-template>
  </xsl:template>
  <!-- William 20140521 usg summary for burstable M2R2 End-->
  
  <!-- amer 20130903 usg summary for burstable -->
  <xsl:template name="event-template-Usage-Sum-burst">
    <xsl:param name="event-list"/>
    <xsl:param name="usg-section"/>
    <xsl:if test="$event-list">
      <xsl:if test="$event-list[contains(PIN_FLD_DESCR,'BURST')]">
        <USG_ITEM>
          <USG_SERVICE>Burstable</USG_SERVICE>
          <USG_SECTION>
            <xsl:call-template name="usage-Heading">
              <xsl:with-param name="usageSection" select="$usg-section"/>
            </xsl:call-template>
          </USG_SECTION>
          <USG_SUB_SECTION>
            <xsl:value-of select="$event-list/PIN_FLD_CALLING_NUMBER" />
          </USG_SUB_SECTION>
          <AMT_TOTAL>
              <xsl:value-of select="format-number(sum($event-list/PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
          </AMT_TOTAL>
        </USG_ITEM>
      </xsl:if>
    </xsl:if>
  </xsl:template>
  
    <!-- William 20140421 usg summary for burstable usage -->
    <xsl:template name="event-template-Usage-Sum-burstable">
    <xsl:param name="event-list"/>
    <xsl:param name="usg-section"/>
    <xsl:if test="$event-list">
        <USG_ITEM>
          <USG_SERVICE>Burstable</USG_SERVICE>
          <USG_SECTION>
            <xsl:call-template name="usage-Heading">
              <xsl:with-param name="usageSection" select="$usg-section"/>
            </xsl:call-template>
          </USG_SECTION>
          <USG_SUB_SECTION>
            <xsl:value-of select="$event-list/PIN_FLD_CALLING_NUMBER" />
          </USG_SUB_SECTION>
          <AMT_TOTAL>
              <xsl:value-of select="format-number(sum($event-list/PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
          </AMT_TOTAL>
        </USG_ITEM>
    </xsl:if>
  </xsl:template>
  
  <xsl:template name="event-template-Usage-Sum">
    <xsl:param name="event-list"/>
    <xsl:param name="usg-section"/>
    <xsl:if test="$event-list">
      <xsl:if test="not($event-list/PIN_FLD_BAL_IMPACTS[contains(PIN_FLD_IMPACT_TYPE,'512')])">
        <USG_ITEM>
          <USG_SERVICE>
            <xsl:choose>
              <xsl:when test="contains($event-list/PIN_FLD_ITEM_OBJ,'usage')">
                <xsl:choose>
                  <xsl:when test="translate(/invoice/PIN_FLD_ACCTINFO/TM_FLD_BILL_LANGUAGE,$lowercase,$uppercase) = 'MAL'">Talian<xsl:if test="contains(substring-before(substring-after($event-list/../PIN_FLD_SERVICE_OBJ,' '),' '), 'sip')"> SIP</xsl:if>
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:if test="contains(substring-before(substring-after($event-list/../PIN_FLD_SERVICE_OBJ,' '),' '), 'sip')">SIP </xsl:if>Voice</xsl:otherwise>
                </xsl:choose>
              </xsl:when>
              <xsl:when test="contains($event-list/PIN_FLD_ITEM_OBJ,'iptv')">HyppTV</xsl:when>
              <xsl:otherwise>
</xsl:otherwise>
            </xsl:choose>
          </USG_SERVICE>
          <USG_SECTION>
            <xsl:call-template name="usage-Heading">
              <xsl:with-param name="usageSection" select="$usg-section"/>
            </xsl:call-template>
          </USG_SECTION>
          <USG_SUB_SECTION>
            <xsl:call-template name="usage-Section">
              <xsl:with-param name="usageSection" select="$usg-section"/>
            </xsl:call-template>
          </USG_SUB_SECTION>
          <AMT_TOTAL>
            <xsl:if test="$event-list/PIN_FLD_TOTAL[@elem  = 1000107]">
              <xsl:value-of select="format-number(sum($event-list/PIN_FLD_TOTAL[@elem  = 1000107]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
            </xsl:if>
            <xsl:if test="$event-list/PIN_FLD_TOTAL[@elem  = $primary-currency-id]">
              <xsl:value-of select="format-number(sum($event-list/PIN_FLD_TOTAL[@elem  = $primary-currency-id]/PIN_FLD_AMOUNT),'###,###,###,##0.00')"/>
            </xsl:if>
          </AMT_TOTAL>
        </USG_ITEM>
      </xsl:if>
    </xsl:if>
  </xsl:template>
  <xsl:template name="bill-Message-Outstanding">
    <xsl:choose>
      <xsl:when test="translate(/invoice/PIN_FLD_ACCTINFO/TM_FLD_BILL_LANGUAGE,$lowercase,$uppercase) = 'MAL'">Bil anda mempunyai tunggakan. Sila jelaskan dengan kadar segera untuk mengelakkan gangguan perkhidmatan sementara.</xsl:when>
      <xsl:otherwise>You have outstanding due. Please settle the dues immediately to avoid any service interruption.</xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="bill-Message-Current">
    <xsl:choose>
      <xsl:when test="translate(/invoice/PIN_FLD_ACCTINFO/TM_FLD_BILL_LANGUAGE,$lowercase,$uppercase) = 'MAL'">Untuk caj semasa, sila jelaskan bayaran sebelum tarikh seperti yang tertera di atas.</xsl:when>
      <xsl:otherwise>For the current charges, kindly remit them before or on the due date stated on your bill.</xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="bill-message-conditional">
    <xsl:if test="invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP = 'CONSUMER' or invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP = 'SME'">
    <BILL_MSG>
		<xsl:choose>
		  <xsl:when test="translate(/invoice/PIN_FLD_ACCTINFO/TM_FLD_BILL_LANGUAGE,$lowercase,$uppercase) = 'MAL'">Mulai 11 SEPTEMBER 2014, tempoh pembayaran bil telah diubah kepada 21 hari. Untuk maklumat lanjut, rujuk halaman pengumuman.</xsl:when>
		  <xsl:otherwise>Starting from 11 SEPTEMBER 2014, the bill payment period has been changed to 21 days. For more details, refer to announcement page.</xsl:otherwise>
		</xsl:choose>
    </BILL_MSG>
    </xsl:if>
    <xsl:if test="invoice/PIN_FLD_ACCTINFO/TM_FLD_BILL_STREAM = '50'">
      <xsl:choose>
        <xsl:when test="translate(/invoice/PIN_FLD_ACCTINFO/TM_FLD_BILL_LANGUAGE,$lowercase,$uppercase) = 'MAL'">
        <BILL_MSG>Tuan/Puan akan menerima cek sejumlah yang tertera pada ruangan jumlah perlu dibayar pada bil tuan.</BILL_MSG>
		<BILL_MSG>Jumlah muktamad akan bergantung kepada semakan akhir kami ke atas bil-bil yang belum dijelaskan lagi dan baki lebihan hanya akan dibayar sekiranya amaun melebihi RM10.</BILL_MSG>
  		  </xsl:when>
  		  <xsl:otherwise>
  		  <BILL_MSG>You will receive cheque as stated in the Total Amount to be Paid.</BILL_MSG>
		  <BILL_MSG>Final amount will depend on the finalized checking on the outstanding bill and only balance amounting more than RM10 will be refunded.</BILL_MSG>
  		  </xsl:otherwise>
		</xsl:choose>
    </xsl:if>
  </xsl:template>
  
  <!-- William 20130425 Targeted Bill M2R2 -->
<xsl:template name="bill-Message-front">
<xsl:param name="event-list" />
<xsl:if test ="$event-list">
<!-- William 20131010 ICARE00012013 -Request for bill message in CMD to appear one space below - Start -->
<BILL_MSG></BILL_MSG>
<!-- William 20131010 ICARE00012013 -Request for bill message in CMD to appear one space below - End -->
<xsl:for-each select="$event-list">
<BILL_MSG><xsl:value-of select="TM_FLD_BODY"/></BILL_MSG>
</xsl:for-each>
</xsl:if>
</xsl:template>
<!-- William 20131115 Post D3 - Announcement messages for Global and Wholesale are separate -->
<xsl:template name="bill-Message-back">
<xsl:param name="event-list" />
<xsl:if test ="$event-list">
<ANNCMNT_SEGMENT>
<xsl:if test="/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP !='Wholesale' and /invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP !='Global' ">
<xsl:for-each select="$event-list">
<MESSAGE>
	<HEADER><xsl:value-of select="TM_FLD_HEADER"/></HEADER>
	<BODY><xsl:value-of select ="TM_FLD_BODY"/></BODY>
</MESSAGE>
</xsl:for-each>
</xsl:if>
<xsl:if test="/invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP ='Wholesale' or /invoice/PIN_FLD_ACCTINFO/TM_FLD_SEGMENT_GROUP ='Global' ">
<xsl:for-each select="$event-list">
<MESSAGE>
	<HEADER><xsl:value-of select="TM_FLD_HEADER"/></HEADER>
	<BODY><xsl:value-of select ="TM_FLD_BODY"/></BODY>
</MESSAGE>
</xsl:for-each>
</xsl:if>
</ANNCMNT_SEGMENT>
</xsl:if>
</xsl:template>
<!-- William 20130425 Targeted Bill Message M2R2 -->
  
  
  <xsl:template name="bill-Message-Announcement">
    <xsl:choose>
      <xsl:when test="translate(/invoice/PIN_FLD_ACCTINFO/TM_FLD_BILL_LANGUAGE,$lowercase,$uppercase) = 'MAL'">Format baru bil TM akan mula diperkenalkan pada bulan April 2014. Untuk maklumat lanjut berkenaan bil berformat baru sila hubungi pengurus akaun / akaun eksekutif anda.</xsl:when>
      <xsl:otherwise>TM new bill format will be introduced in April 2014. For more information on your new bill, please refer to your account managers / executives.</xsl:otherwise>
    </xsl:choose>
<!-- amer - obselete -->
    <!-- xsl:choose>
      <xsl:when test="translate(/invoice/PIN_FLD_ACCTINFO/TM_FLD_BILL_LANGUAGE,$lowercase,$uppercase) = 'MAL'">
        <BILL_MSG>POLISI BARU PENGAKTIFAN SEMULA PERKHIDMATAN - BAYARAN 100% (rujuk ruangan 'PENGUMUMAN' untuk maklumat penuh)</BILL_MSG>
      </xsl:when>
      <xsl:otherwise>
        <BILL_MSG>NEW SERVICE REACTIVATION POLICY - 100% PAYMENT (refer to 'ANNOUNCEMENT' column for full details)</BILL_MSG>
      </xsl:otherwise>
    </xsl:choose -->
  </xsl:template>
    <xsl:template name="bill-Message-Announcement-Second">
    <xsl:choose>
      <xsl:when test="translate(/invoice/PIN_FLD_ACCTINFO/TM_FLD_BILL_LANGUAGE,$lowercase,$uppercase) = 'MAL'">Format baru bil TM akan mula diperkenalkan pada bulan Ogos 2014. Untuk maklumat lanjut berkenaan bil berformat baru sila hubungi pengurus akaun / akaun eksekutif anda.</xsl:when>
      <xsl:otherwise>TM new bill format will be introduced in August 2014. For more information on your new bill, please refer to your account managers / executives.</xsl:otherwise>
    </xsl:choose>
<!-- amer - obselete -->
    <!-- xsl:choose>
      <xsl:when test="translate(/invoice/PIN_FLD_ACCTINFO/TM_FLD_BILL_LANGUAGE,$lowercase,$uppercase) = 'MAL'">
        <BILL_MSG>POLISI BARU PENGAKTIFAN SEMULA PERKHIDMATAN - BAYARAN 100% (rujuk ruangan 'PENGUMUMAN' untuk maklumat penuh)</BILL_MSG>
      </xsl:when>
      <xsl:otherwise>
        <BILL_MSG>NEW SERVICE REACTIVATION POLICY - 100% PAYMENT (refer to 'ANNOUNCEMENT' column for full details)</BILL_MSG>
      </xsl:otherwise>
    </xsl:choose -->
  </xsl:template>  
  <!--  Convert seconds into duration  -->
  <xsl:template name="format-duration">
    <xsl:param name="seconds"/>
    <xsl:variable name="seconds-per-hour" select="3600"/>
    <xsl:variable name="seconds-per-minute" select="60"/>
    <xsl:variable name="hours" select="floor($seconds div $seconds-per-hour)"/>
    <xsl:variable name="minutes" select="floor(($seconds - $hours * $seconds-per-hour) div $seconds-per-minute)"/>
    <xsl:variable name="remaining-seconds" select="$seconds - $hours * $seconds-per-hour - $minutes * $seconds-per-minute"/>
    <xsl:value-of select="format-number($hours, '00')"/>
    <xsl:text>:</xsl:text>
    <xsl:value-of select="format-number($minutes, '00')"/>
    <xsl:text>:</xsl:text>
    <xsl:value-of select="format-number($remaining-seconds, '00')"/>
  </xsl:template>
  <!--  Format usage date  -->
  <xsl:template name="format-usage-date">
    <xsl:param name="date"/>
    <xsl:value-of select="substring($date,7,2)"/>/<xsl:value-of select="substring($date,5,2)"/>/<xsl:value-of select="substring($date,1,4)"/>
  </xsl:template>
  <!--  Format usage time  -->
  <xsl:template name="format-usage-time">
    <xsl:param name="date"/>
    <xsl:value-of select="substring($date,9,2)"/>:<xsl:value-of select="substring($date,11,2)"/>:<xsl:value-of select="substring($date,13,4)"/>
  </xsl:template>
  <xsl:template name="format-unix-date">
    <xsl:param name="unix"/>
    <!--   +8HRS for GMT corrections   -->
    <!--   seconds stored in BRM is +0GMT   -->
    <xsl:variable name="unix-gmt" select="$unix + 28800 - $yearOffsetInSecs"/>
    <xsl:call-template name="date-loop">
      <xsl:with-param name="cYear" select="$yearOffset"/>
      <xsl:with-param name="numRemain" select="$unix-gmt"/>
    </xsl:call-template>
  </xsl:template>
  <!-- format date to dd-mm-yyyy -->
  <xsl:template name="format-unix-date-tm-reward">
    <xsl:param name="unix"/>
    <!--   +8HRS for GMT corrections   -->
    <!--   seconds stored in BRM is +0GMT   -->
    <xsl:variable name="unix-gmt" select="$unix + 28800 - $yearOffsetInSecs"/>
    <xsl:call-template name="date-loop">
      <xsl:with-param name="cYear" select="$yearOffset"/>
      <xsl:with-param name="numRemain" select="$unix-gmt"/>
    </xsl:call-template>
  </xsl:template>
  <!--   Format date in DD MMM YYYY   -->
  <xsl:template name="format-unix-date-invoice-date">
    <xsl:param name="unix"/>
    <!--   +8HRS for GMT corrections   -->
    <!--   seconds stored in BRM is +0GMT   -->
    <xsl:variable name="unix-gmt" select="$unix + 28800 - $yearOffsetInSecs"/>
    <xsl:call-template name="date-loop-mmm">
      <xsl:with-param name="cYear" select="$yearOffset"/>
      <xsl:with-param name="numRemain" select="$unix-gmt"/>
    </xsl:call-template>
  </xsl:template>
  <!-- Accurate Recursive Date Formatter for Epoch Time -->
  <!-- David Khaw 17 Aug 2012 -->
  <xsl:template name="date-loop">
    <xsl:param name="cYear"/>
    <xsl:param name="numRemain"/>
    <xsl:variable name="daysInCYear">
      <xsl:choose>
        <xsl:when test="$cYear mod 4 = 0 and ($cYear mod 100 != 0 or $cYear mod 400 = 0)">
          <xsl:value-of select="31622400"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="31536000"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="nextCYear" select="$cYear + 1"/>
    <xsl:variable name="nextNumRemain" select="$numRemain - $daysInCYear"/>
    <xsl:variable name="daysNextInCYear">
      <xsl:choose>
        <xsl:when test="$nextCYear mod 4 = 0 and ($nextCYear mod 100 != 0 or $nextCYear mod 400 = 0)">
          <xsl:value-of select="31622400"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="31536000"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="$nextNumRemain >= $daysNextInCYear">
        <!-- Recurse further -->
        <xsl:call-template name="date-loop">
          <xsl:with-param name="cYear" select="$nextCYear"/>
          <xsl:with-param name="numRemain" select="$nextNumRemain"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <!-- Display Month and Day -->
        <xsl:variable name="daysInYear" select="floor($nextNumRemain div 86400) + 1"/>
        <xsl:choose>
          <xsl:when test="$nextCYear mod 4 = 0 and ($nextCYear mod 100 != 0 or $nextCYear mod 400 = 0)">
            <!-- LEAP YEAR -->
            <xsl:variable name="month">
              <xsl:choose>
                <xsl:when test="$daysInYear > 335">12</xsl:when>
                <xsl:when test="$daysInYear > 305">11</xsl:when>
                <xsl:when test="$daysInYear > 274">10</xsl:when>
                <xsl:when test="$daysInYear > 244">09</xsl:when>
                <xsl:when test="$daysInYear > 213">08</xsl:when>
                <xsl:when test="$daysInYear > 182">07</xsl:when>
                <xsl:when test="$daysInYear > 152">06</xsl:when>
                <xsl:when test="$daysInYear > 121">05</xsl:when>
                <xsl:when test="$daysInYear >  91">04</xsl:when>
                <xsl:when test="$daysInYear > 60">03</xsl:when>
                <xsl:when test="$daysInYear > 31">02</xsl:when>
                <xsl:otherwise>01</xsl:otherwise>
              </xsl:choose>
            </xsl:variable>
            <xsl:variable name="date">
              <xsl:value-of select="$daysInYear - substring('000031060091121152182213244274305335', 3 * $month - 2, 3)"/>
            </xsl:variable>
            <!-- DISPLAY ALL DATE -->
            <xsl:if test="$date &lt; 10">0</xsl:if>
            <xsl:value-of select="$date"/>/<xsl:value-of select="$month"/>/<xsl:value-of select="$nextCYear"/>
          </xsl:when>
          <xsl:otherwise>
            <!-- NOT LEAP YEAR -->
            <xsl:variable name="month">
              <xsl:choose>
                <xsl:when test="$daysInYear > 334">12</xsl:when>
                <xsl:when test="$daysInYear > 304">11</xsl:when>
                <xsl:when test="$daysInYear > 273">10</xsl:when>
                <xsl:when test="$daysInYear > 243">09</xsl:when>
                <xsl:when test="$daysInYear > 212">08</xsl:when>
                <xsl:when test="$daysInYear > 181">07</xsl:when>
                <xsl:when test="$daysInYear > 151">06</xsl:when>
                <xsl:when test="$daysInYear > 120">05</xsl:when>
                <xsl:when test="$daysInYear >  90">04</xsl:when>
                <xsl:when test="$daysInYear > 59">03</xsl:when>
                <xsl:when test="$daysInYear > 31">02</xsl:when>
                <xsl:otherwise>01</xsl:otherwise>
              </xsl:choose>
            </xsl:variable>
            <xsl:variable name="date">
              <xsl:value-of select="$daysInYear - substring('000031059090120151181212243273304334', 3 * $month - 2, 3)"/>
            </xsl:variable>
            <!-- DISPLAY ALL DATE -->
            <xsl:if test="$date &lt; 10">0</xsl:if>
            <xsl:value-of select="$date"/>/<xsl:value-of select="$month"/>/<xsl:value-of select="$nextCYear"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <!-- Accurate Recursive Date Formatter for Epoch Time with MMM Months -->
  <!-- David Khaw 17 Aug 2012 -->
  <xsl:template name="date-loop-mmm">
    <xsl:param name="cYear"/>
    <xsl:param name="numRemain"/>
    <xsl:variable name="daysInCYear">
      <xsl:choose>
        <xsl:when test="$cYear mod 4 = 0 and ($cYear mod 100 != 0 or $cYear mod 400 = 0)">
          <xsl:value-of select="31622400"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="31536000"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="nextCYear" select="$cYear + 1"/>
    <xsl:variable name="nextNumRemain" select="$numRemain - $daysInCYear"/>
    <xsl:variable name="daysNextInCYear">
      <xsl:choose>
        <xsl:when test="$nextCYear mod 4 = 0 and ($nextCYear mod 100 != 0 or $nextCYear mod 400 = 0)">
          <xsl:value-of select="31622400"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="31536000"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="$nextNumRemain >= $daysNextInCYear">
        <!-- Recurse further -->
        <xsl:call-template name="date-loop-mmm">
          <xsl:with-param name="cYear" select="$nextCYear"/>
          <xsl:with-param name="numRemain" select="$nextNumRemain"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <!-- Display Month and Day -->
        <xsl:variable name="daysInYear" select="floor($nextNumRemain div 86400) + 1"/>
        <xsl:choose>
          <xsl:when test="$nextCYear mod 4 = 0 and ($nextCYear mod 100 != 0 or $nextCYear mod 400 = 0)">
            <!-- LEAP YEAR -->
            <xsl:variable name="month">
              <xsl:choose>
                <xsl:when test="$daysInYear > 335">12</xsl:when>
                <xsl:when test="$daysInYear > 305">11</xsl:when>
                <xsl:when test="$daysInYear > 274">10</xsl:when>
                <xsl:when test="$daysInYear > 244">09</xsl:when>
                <xsl:when test="$daysInYear > 213">08</xsl:when>
                <xsl:when test="$daysInYear > 182">07</xsl:when>
                <xsl:when test="$daysInYear > 152">06</xsl:when>
                <xsl:when test="$daysInYear > 121">05</xsl:when>
                <xsl:when test="$daysInYear >  91">04</xsl:when>
                <xsl:when test="$daysInYear > 60">03</xsl:when>
                <xsl:when test="$daysInYear > 31">02</xsl:when>
                <xsl:otherwise>01</xsl:otherwise>
              </xsl:choose>
            </xsl:variable>
            <xsl:variable name="date">
              <xsl:value-of select="$daysInYear - substring('000031060091121152182213244274305335', 3 * $month - 2, 3)"/>
            </xsl:variable>
            <xsl:variable name="month_MMM">
              <xsl:choose>
                <xsl:when test="$languagePreference = 'ENG'">
                  <xsl:choose>
                    <xsl:when test="$month = '01'">JAN</xsl:when>
                    <xsl:when test="$month = '02'">FEB</xsl:when>
                    <xsl:when test="$month = '03'">MAR</xsl:when>
                    <xsl:when test="$month = '04'">APR</xsl:when>
                    <xsl:when test="$month = '05'">MAY</xsl:when>
                    <xsl:when test="$month = '06'">JUN</xsl:when>
                    <xsl:when test="$month = '07'">JUL</xsl:when>
                    <xsl:when test="$month = '08'">AUG</xsl:when>
                    <xsl:when test="$month = '09'">SEP</xsl:when>
                    <xsl:when test="$month = '10'">OCT</xsl:when>
                    <xsl:when test="$month = '11'">NOV</xsl:when>
                    <xsl:when test="$month = '12'">DEC</xsl:when>
                    <xsl:otherwise>
</xsl:otherwise>
                  </xsl:choose>
                </xsl:when>
                <xsl:when test="$languagePreference = 'MAL'">
                  <xsl:choose>
                    <xsl:when test="$month = '01'">JAN</xsl:when>
                    <xsl:when test="$month = '02'">FEB</xsl:when>
                    <xsl:when test="$month = '03'">MAC</xsl:when>
                    <xsl:when test="$month = '04'">APR</xsl:when>
                    <xsl:when test="$month = '05'">MEI</xsl:when>
                    <xsl:when test="$month = '06'">JUN</xsl:when>
                    <xsl:when test="$month = '07'">JUL</xsl:when>
                    <xsl:when test="$month = '08'">OGO</xsl:when>
                    <xsl:when test="$month = '09'">SEP</xsl:when>
                    <xsl:when test="$month = '10'">OKT</xsl:when>
                    <xsl:when test="$month = '11'">NOV</xsl:when>
                    <xsl:when test="$month = '12'">DIS</xsl:when>
                    <xsl:otherwise>
</xsl:otherwise>
                  </xsl:choose>
                </xsl:when>
              </xsl:choose>
            </xsl:variable>
            <!-- DISPLAY ALL DATE -->
            <xsl:if test="$date &lt; 10">0</xsl:if>
            <xsl:value-of select="$date"/>
            <xsl:text> </xsl:text>
            <xsl:value-of select="$month_MMM"/>
            <xsl:text> </xsl:text>
            <xsl:value-of select="$nextCYear"/>
          </xsl:when>
          <xsl:otherwise>
            <!-- NOT LEAP YEAR -->
            <xsl:variable name="month">
              <xsl:choose>
                <xsl:when test="$daysInYear > 334">12</xsl:when>
                <xsl:when test="$daysInYear > 304">11</xsl:when>
                <xsl:when test="$daysInYear > 273">10</xsl:when>
                <xsl:when test="$daysInYear > 243">09</xsl:when>
                <xsl:when test="$daysInYear > 212">08</xsl:when>
                <xsl:when test="$daysInYear > 181">07</xsl:when>
                <xsl:when test="$daysInYear > 151">06</xsl:when>
                <xsl:when test="$daysInYear > 120">05</xsl:when>
                <xsl:when test="$daysInYear >  90">04</xsl:when>
                <xsl:when test="$daysInYear > 59">03</xsl:when>
                <xsl:when test="$daysInYear > 31">02</xsl:when>
                <xsl:otherwise>01</xsl:otherwise>
              </xsl:choose>
            </xsl:variable>
            <xsl:variable name="date">
              <xsl:value-of select="$daysInYear - substring('000031059090120151181212243273304334', 3 * $month - 2, 3)"/>
            </xsl:variable>
            <xsl:variable name="month_MMM">
              <xsl:choose>
                <xsl:when test="$languagePreference = 'ENG'">
                  <xsl:choose>
                    <xsl:when test="$month = '01'">JAN</xsl:when>
                    <xsl:when test="$month = '02'">FEB</xsl:when>
                    <xsl:when test="$month = '03'">MAR</xsl:when>
                    <xsl:when test="$month = '04'">APR</xsl:when>
                    <xsl:when test="$month = '05'">MAY</xsl:when>
                    <xsl:when test="$month = '06'">JUN</xsl:when>
                    <xsl:when test="$month = '07'">JUL</xsl:when>
                    <xsl:when test="$month = '08'">AUG</xsl:when>
                    <xsl:when test="$month = '09'">SEP</xsl:when>
                    <xsl:when test="$month = '10'">OCT</xsl:when>
                    <xsl:when test="$month = '11'">NOV</xsl:when>
                    <xsl:when test="$month = '12'">DEC</xsl:when>
                    <xsl:otherwise>
</xsl:otherwise>
                  </xsl:choose>
                </xsl:when>
                <xsl:when test="$languagePreference = 'MAL'">
                  <xsl:choose>
                    <xsl:when test="$month = '01'">JAN</xsl:when>
                    <xsl:when test="$month = '02'">FEB</xsl:when>
                    <xsl:when test="$month = '03'">MAC</xsl:when>
                    <xsl:when test="$month = '04'">APR</xsl:when>
                    <xsl:when test="$month = '05'">MEI</xsl:when>
                    <xsl:when test="$month = '06'">JUN</xsl:when>
                    <xsl:when test="$month = '07'">JUL</xsl:when>
                    <xsl:when test="$month = '08'">OGO</xsl:when>
                    <xsl:when test="$month = '09'">SEP</xsl:when>
                    <xsl:when test="$month = '10'">OKT</xsl:when>
                    <xsl:when test="$month = '11'">NOV</xsl:when>
                    <xsl:when test="$month = '12'">DIS</xsl:when>
                    <xsl:otherwise>
</xsl:otherwise>
                  </xsl:choose>
                </xsl:when>
              </xsl:choose>
            </xsl:variable>
            <!-- DISPLAY ALL DATE -->
            <xsl:if test="$date &lt; 10">0</xsl:if>
            <xsl:value-of select="$date"/>
            <xsl:text> </xsl:text>
            <xsl:value-of select="$month_MMM"/>
            <xsl:text> </xsl:text>
            <xsl:value-of select="$nextCYear"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


<!-- Date formate DDMMYYY -->
<xsl:template name="format-unix-date-bc-date">
<xsl:param name="unix" />

		<!--   +8HRS for GMT corrections   -->
    <!--   seconds stored in BRM is +0GMT   -->
		<xsl:variable name="unix-gmt" select="$unix + 28800"/> 

		<!--   Calculate number of leap years that have passed before the previous year   -->
		<xsl:variable name="unix-numleapdays" select="floor(($unix-gmt - 94694400) div 126230400) + 1"/> 
				<!--   Year, taking previous leap years into account but not taking into account that current year might be a leap year   -->
		<xsl:variable name="year-temp" select="floor(($unix-gmt - $unix-numleapdays * 86400) div 31536000) + 1970" />
				<!--   Meaningless most of the time; on 31st December of a leap year, gives a value between 1 and 86399 indicating the
		     number of seconds we are beyond a 365-day year; $year-temp above will incorrectly give the following year on
			 31st December of leap years because the year has more than 31536000 seconds, so this is used as a correction
			 factor   -->
    <xsl:variable name="extra-seconds-this-year" select="$unix-gmt - $unix-numleapdays * 86400 - ($year-temp - 1970) * 31536000" /> 
    
    		<xsl:variable name="year">
			<xsl:choose>
				<xsl:when test="($year-temp mod 4 = 1)  and $extra-seconds-this-year > 0 and $extra-seconds-this-year &lt; 86400">
					<xsl:value-of select="$year-temp - 1" />
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="$year-temp" />
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		
				<xsl:variable name="hour" select="floor(($unix-gmt mod 86400) div 3600)" />
		<xsl:variable name="minute" select="floor(($unix-gmt mod 3600) div 60)" />
		<xsl:variable name="second" select="$unix-gmt mod 60" />
		
		
				<xsl:variable name="yday" select="floor(($unix-gmt - ($year - 1970)*31536000) div 86400) - $unix-numleapdays + 1" />
		
				<xsl:variable name="yday-leap">
			<xsl:choose>
				<xsl:when test="$yday >= 60 and $year mod 4 = 0">
					<xsl:value-of select="$yday - 1" />
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="$yday" />
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		
				<!--   Work out month from leap-adjusted year day   -->
		<xsl:variable name="month">
			<xsl:choose>
        <xsl:when test="$yday-leap > 334">12</xsl:when>
				<xsl:when test="$yday-leap > 304">11</xsl:when>
        <xsl:when test="$yday-leap > 273">10</xsl:when>
        <xsl:when test="$yday-leap > 243">09</xsl:when>
				<xsl:when test="$yday-leap > 212">08</xsl:when>
        <xsl:when test="$yday-leap > 181">07</xsl:when>
        <xsl:when test="$yday-leap > 151">06</xsl:when>
        <xsl:when test="$yday-leap > 120">05</xsl:when>
        <xsl:when test="$yday-leap >  90">04</xsl:when>
				<xsl:when test="$yday-leap > 59">03</xsl:when>
        <xsl:when test="$yday-leap > 31">02</xsl:when>
				<xsl:otherwise>01</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		
				<!--   Lookup date from table; day 60 of leap years is 29th February   -->
		<xsl:variable name="date">
			<xsl:choose>
				<xsl:when test="$yday != 60 or $year mod 4 != 0">
					<xsl:value-of select="$yday-leap - substring('000031059090120151181212243273304334', 3 * $month - 2, 3)" />
				</xsl:when>
				<xsl:otherwise>29</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		
<xsl:if test="$date &lt; 10">0</xsl:if><xsl:value-of select="$date" /><xsl:value-of select="$month" /><xsl:value-of select="$year" />
</xsl:template>


  <!--   Usage section mapping   -->
  <xsl:template name="usage-Section">
    <xsl:param name="usageSection"/>
    <xsl:choose>
      <xsl:when test="$languagePreference = 'ENG'">
        <xsl:choose>
          <xsl:when test="$usageSection = 'NL01'">To Fixed</xsl:when>
          <xsl:when test="$usageSection = 'NL02'">To Mobile</xsl:when>
          <xsl:when test="$usageSection = 'IL01'">To Fixed</xsl:when>
          <xsl:when test="$usageSection = 'IL02'">To Mobile</xsl:when>
          <xsl:when test="$usageSection = 'NA01'">Operator</xsl:when>
          <xsl:when test="$usageSection = 'NA02'">Reversed Charged</xsl:when>
          <xsl:when test="$usageSection = 'NA03'">Callpoint</xsl:when>
          <xsl:when test="$usageSection = 'NA04'">Directory 103</xsl:when>
          <xsl:when test="$usageSection = 'NA05'">Directory 103-Connected</xsl:when>
          <xsl:when test="$usageSection = 'IT01'">Operator</xsl:when>
          <xsl:when test="$usageSection = 'IT02'">Reversed Charged</xsl:when>
          <xsl:when test="$usageSection = 'IT03'">Malaysia Direct</xsl:when>
          <xsl:when test="$usageSection = 'SOD'">SOD</xsl:when>
          <xsl:when test="$usageSection = 'VOD'">VOD</xsl:when>
          <xsl:when test="$usageSection = 'ID00'">INTERACTIVE</xsl:when>
          <xsl:when test="$usageSection = 'IB00'">
            <xsl:value-of select="$total_SMS"/> SMS</xsl:when>
          <xsl:when test="$usageSection = 'WB00'">BURSTABLE</xsl:when>
          <xsl:otherwise>
</xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:when test="$languagePreference = 'MAL'">
        <xsl:choose>
          <xsl:when test="$usageSection = 'NL01'">Ke Talian Tetap</xsl:when>
          <xsl:when test="$usageSection = 'NL02'">Ke Talian Mudah Alih</xsl:when>
          <xsl:when test="$usageSection = 'IL01'">Ke Talian Tetap</xsl:when>
          <xsl:when test="$usageSection = 'IL02'">Ke Talian Mudah Alih</xsl:when>
          <xsl:when test="$usageSection = 'NA01'">Telefonis</xsl:when>
          <xsl:when test="$usageSection = 'NA02'">Panggilan Pindah Bayaran</xsl:when>
          <xsl:when test="$usageSection = 'NA03'">Callpoint</xsl:when>
          <xsl:when test="$usageSection = 'NA04'">Direktori 103</xsl:when>
          <xsl:when test="$usageSection = 'NA05'">Direktori 103-Sambungan</xsl:when>
          <xsl:when test="$usageSection = 'IT01'">Telefonis</xsl:when>
          <xsl:when test="$usageSection = 'IT02'">Panggilan Pindah Bayaran</xsl:when>
          <xsl:when test="$usageSection = 'IT03'">Malaysia Direct</xsl:when>
          <xsl:when test="$usageSection = 'SOD'">SOD</xsl:when>
          <xsl:when test="$usageSection = 'VOD'">VOD</xsl:when>
          <xsl:when test="$usageSection = 'ID00'">INTERAKTIF</xsl:when>
          <xsl:when test="$usageSection = 'IB00'">
            <xsl:value-of select="$total_SMS"/> SMS</xsl:when>
          <xsl:otherwise>
</xsl:otherwise>
        </xsl:choose>
      </xsl:when>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="usage-Section-detail">
    <xsl:param name="usageSection"/>
    <xsl:choose>
      <xsl:when test="$languagePreference = 'ENG'">
        <xsl:choose>
          <xsl:when test="$usageSection = 'NL01'">To Fixed</xsl:when>
          <xsl:when test="$usageSection = 'NL02'">To Mobile</xsl:when>
          <xsl:when test="$usageSection = 'IL01'">To Fixed</xsl:when>
          <xsl:when test="$usageSection = 'IL02'">To Mobile</xsl:when>
          <xsl:when test="$usageSection = 'NA01'">Operator</xsl:when>
          <xsl:when test="$usageSection = 'NA02'">Reversed Charged</xsl:when>
          <xsl:when test="$usageSection = 'NA03'">Callpoint</xsl:when>
          <xsl:when test="$usageSection = 'NA04'">Directory 103</xsl:when>
          <xsl:when test="$usageSection = 'NA05'">Directory 103-Connected</xsl:when>
          <xsl:when test="$usageSection = 'IT01'">Operator</xsl:when>
          <xsl:when test="$usageSection = 'IT02'">Reversed Charged</xsl:when>
          <xsl:when test="$usageSection = 'IT03'">Malaysia Direct</xsl:when>
          <xsl:when test="$usageSection = 'SOD'">SOD</xsl:when>
          <xsl:when test="$usageSection = 'VOD'">VOD</xsl:when>
          <xsl:when test="$usageSection = 'ID00'">INTERACTIVE</xsl:when>
          <xsl:when test="$usageSection = 'IB00'">
            <xsl:value-of select="$total_SMS"/>
          </xsl:when>
          <xsl:otherwise>
</xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:when test="$languagePreference = 'MAL'">
        <xsl:choose>
          <xsl:when test="$usageSection = 'NL01'">Ke Talian Tetap</xsl:when>
          <xsl:when test="$usageSection = 'NL02'">Ke Talian Mudah Alih</xsl:when>
          <xsl:when test="$usageSection = 'IL01'">Ke Talian Tetap</xsl:when>
          <xsl:when test="$usageSection = 'IL02'">Ke Talian Mudah Alih</xsl:when>
          <xsl:when test="$usageSection = 'NA01'">Telefonis</xsl:when>
          <xsl:when test="$usageSection = 'NA02'">Panggilan Pindah Bayaran</xsl:when>
          <xsl:when test="$usageSection = 'NA03'">Callpoint</xsl:when>
          <xsl:when test="$usageSection = 'NA04'">Direktori 103</xsl:when>
          <xsl:when test="$usageSection = 'NA05'">Direktori 103-Sambungan</xsl:when>
          <xsl:when test="$usageSection = 'IT01'">Telefonis</xsl:when>
          <xsl:when test="$usageSection = 'IT02'">Panggilan Pindah Bayaran</xsl:when>
          <xsl:when test="$usageSection = 'IT03'">Malaysia Direct</xsl:when>
          <xsl:when test="$usageSection = 'SOD'">SOD</xsl:when>
          <xsl:when test="$usageSection = 'VOD'">VOD</xsl:when>
          <xsl:when test="$usageSection = 'ID00'">INTERAKTIF</xsl:when>
          <xsl:when test="$usageSection = 'IB00'">
            <xsl:value-of select="$total_SMS"/>
          </xsl:when>
          <xsl:otherwise>
</xsl:otherwise>
        </xsl:choose>
      </xsl:when>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="usage-Heading">
    <xsl:param name="usageSection"/>
    <xsl:choose>
      <xsl:when test="$languagePreference = 'ENG'">
        <xsl:choose>
          <xsl:when test="contains($usageSection,'NL')">NATIONAL</xsl:when>
          <xsl:when test="contains($usageSection,'IL')">INTERNATIONAL</xsl:when>
          <xsl:when test="contains($usageSection,'NA')">NATIONAL OPERATOR</xsl:when>
          <xsl:when test="contains($usageSection,'IT')">INTERNATIONAL OPERATOR</xsl:when>
          <xsl:when test="contains($usageSection,'OS')">SPECIAL NUMBERS / OTHERS</xsl:when>
          <xsl:when test="contains($usageSection,'SOD')">SOD</xsl:when>
          <xsl:when test="contains($usageSection,'VOD')">VOD</xsl:when>
          <xsl:when test="contains($usageSection,'IB00')">InfoBlast</xsl:when>
          <xsl:when test="contains($usageSection,'ID00')">INTERACTIVE</xsl:when>
          <xsl:when test="contains($usageSection,'WB00')">BURSTABLE</xsl:when>
		  <xsl:when test="contains($usageSection,'OC01')">BURSTABLE</xsl:when>
          <xsl:otherwise>
</xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:when test="$languagePreference = 'MAL'">
        <xsl:choose>
          <xsl:when test="contains($usageSection,'NL')">NASIONAL</xsl:when>
          <xsl:when test="contains($usageSection,'IL')">ANTARABANGSA</xsl:when>
          <xsl:when test="contains($usageSection,'NA')">BANTUAN TELEFONIS NASIONAL</xsl:when>
          <xsl:when test="contains($usageSection,'IT')">BANTUAN TELEFONIS ANTARABANGSA</xsl:when>
          <xsl:when test="contains($usageSection,'OS')">NOMBOR KHAS / LAIN-LAIN</xsl:when>
          <xsl:when test="contains($usageSection,'SOD')">SOD</xsl:when>
          <xsl:when test="contains($usageSection,'VOD')">VOD</xsl:when>
          <xsl:when test="contains($usageSection,'IB00')">InfoBlast</xsl:when>
          <xsl:when test="contains($usageSection,'ID00')">INTERAKTIF</xsl:when>
		  <xsl:when test="contains($usageSection,'OC01')">BURSTABLE</xsl:when>
          <xsl:otherwise>
</xsl:otherwise>
        </xsl:choose>
      </xsl:when>
    </xsl:choose>
  </xsl:template>
  <!--   Special numbers / Others   -->
  <xsl:template name="usage-Category">
    <xsl:param name="usageCategory"/>
    <!--   If not in the list, leave blank  -->
    <xsl:choose>
      <xsl:when test="$usageCategory = 'OS01'">One Number 1-700</xsl:when>
      <xsl:when test="$usageCategory = 'OS02'">Softpin</xsl:when>
      <xsl:when test="$usageCategory = 'OS03'">Mass Calling</xsl:when>
      <xsl:when test="$usageCategory = 'OS04'">MyVoice</xsl:when>
      <xsl:when test="$usageCategory = 'OS05'">MyVoiceBroadcast</xsl:when>
      <xsl:when test="$usageCategory = 'OS06'">TM SPECIAL NUMBER</xsl:when>
      <xsl:when test="$usageCategory = 'OS07'">TIME ANNOUNCEMENT</xsl:when>
      <xsl:when test="$usageCategory = 'OS08'">1055</xsl:when>
      <xsl:when test="$usageCategory = 'OS09'">TENAGA NASIONAL BERHAD HOTLINE</xsl:when>
      <xsl:when test="$usageCategory = 'OS10'">TALIAN NUR</xsl:when>
      <xsl:when test="$usageCategory = 'OS11'">600 Premium Services</xsl:when>
      <xsl:when test="$usageCategory = 'OS12'">Phonogram</xsl:when>
      <xsl:when test="$usageCategory = 'OS13'">Emergency Calls</xsl:when>
      <xsl:when test="$usageCategory = 'OS00'">Others</xsl:when>
      <xsl:when test="$usageCategory = 'ID00'">INTERACTIVE</xsl:when>
      <xsl:otherwise>
</xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="substring-after-last">
    <xsl:param name="string"/>
    <xsl:param name="delimiter"/>
    <xsl:choose>
      <xsl:when test="contains($string, $delimiter)">
        <xsl:call-template name="substring-after-last">
          <xsl:with-param name="string" select="substring-after($string, $delimiter)"/>
          <xsl:with-param name="delimiter" select="$delimiter"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$string"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

<xsl:template name="prepend-pad">    
  <!-- recursive template to right justify and prepend-->
  <!-- the value with whatever padChar is passed in   -->
    <xsl:param name="padChar"> </xsl:param>
    <xsl:param name="padVar"/>
    <xsl:param name="length"/>
    <xsl:choose>
      <xsl:when test="string-length($padVar) &lt; $length">
        <xsl:call-template name="prepend-pad">
          <xsl:with-param name="padChar" select="$padChar"/>
          <xsl:with-param name="padVar" select="concat($padChar,$padVar)"/>
          <xsl:with-param name="length" select="$length"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of 
	  select="substring($padVar,string-length($padVar) -
	  $length + 1)"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
</xsl:stylesheet>
