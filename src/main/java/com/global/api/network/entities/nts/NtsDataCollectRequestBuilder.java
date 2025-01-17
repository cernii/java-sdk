package com.global.api.network.entities.nts;

import com.global.api.builders.TransactionBuilder;
import com.global.api.entities.enums.DebitAuthorizerCode;
import com.global.api.entities.enums.NTSEntryMethod;
import com.global.api.entities.enums.TransactionType;
import com.global.api.entities.exceptions.BatchFullException;
import com.global.api.network.abstractions.IBatchProvider;
import com.global.api.network.entities.NtsObjectParam;
import com.global.api.network.enums.NTSCardTypes;
import com.global.api.paymentMethods.GiftCard;
import com.global.api.paymentMethods.ICardData;
import com.global.api.paymentMethods.IPaymentMethod;
import com.global.api.paymentMethods.ITrackData;
import com.global.api.utils.MessageWriter;
import com.global.api.utils.NtsUtils;
import com.global.api.utils.StringUtils;

public class NtsDataCollectRequestBuilder implements INtsRequestMessage {
    @Override
    public MessageWriter setNtsRequestMessage(NtsObjectParam ntsObjectParam) throws BatchFullException {
        TransactionBuilder builder = ntsObjectParam.getNtsBuilder();
        MessageWriter request = ntsObjectParam.getNtsRequest();
        NTSCardTypes cardType = ntsObjectParam.getNtsCardType();
        String userData = ntsObjectParam.getNtsUserData();
        NtsRequestMessageHeader ntsRequestMessageHeader = builder.getNtsRequestMessageHeader();

        IPaymentMethod paymentMethod = builder.getPaymentMethod();
        if (paymentMethod instanceof ITrackData) {
            ITrackData trackData = (ITrackData) builder.getPaymentMethod();
            if (trackData.getEntryMethod() != null) {
                NTSEntryMethod entryMethod=NtsUtils.isAttendedOrUnattendedEntryMethod(trackData.getEntryMethod(),trackData.getTrackNumber(),ntsObjectParam.getNtsAcceptorConfig().getOperatingEnvironment());
                request.addRange(entryMethod.getValue(), 1);
                NtsUtils.log("Entry Method", entryMethod.getValue());
            } else {
                request.addRange(NTSEntryMethod.MagneticStripeWithoutTrackDataAttended.getValue(), 1);
                NtsUtils.log("Entry Method", NTSEntryMethod.MagneticStripeWithoutTrackDataAttended.getValue());
            }
        } else if (paymentMethod instanceof ICardData) {
            request.addRange(NTSEntryMethod.MagneticStripeWithoutTrackDataAttended.getValue(), 1);
            NtsUtils.log("Entry Method", NTSEntryMethod.MagneticStripeWithoutTrackDataAttended.getValue());
        } else if (paymentMethod instanceof GiftCard) {
            request.addRange(NTSEntryMethod.MagneticStripeTrack1DataAttended.getValue(), 1);
            NtsUtils.log("Entry Method", NTSEntryMethod.MagneticStripeTrack1DataAttended.getValue());
        }

        // Card Type
        if (cardType != null) {
            NtsUtils.log("CardType : ", cardType.getValue());
            request.addRange(cardType.getValue(), 2);
        }
        NtsDataCollectRequest ntsDataCollectRequest = builder.getNtsDataCollectRequest();
        if (ntsDataCollectRequest != null) {

            IBatchProvider batchProvider = ntsObjectParam.getNtsBatchProvider();
            int batchNumber = builder.getBatchNumber();
            int sequenceNumber = 0;

            if (!StringUtils.isNullOrEmpty(ntsDataCollectRequest.getDebitAuthorizer())) {
                request.addRange(ntsDataCollectRequest.getDebitAuthorizer(), 2); // Response value from ebt or pin debit authorizer
                NtsUtils.log("DebitAuthorizer", ntsDataCollectRequest.getDebitAuthorizer());
            } else {
                request.addRange(DebitAuthorizerCode.NonPinDebitCard.getValue(), 2);
                NtsUtils.log("DebitAuthorizer", DebitAuthorizerCode.NonPinDebitCard.getValue());
            }

            if (paymentMethod instanceof ICardData) {
                ICardData cardData = (ICardData) paymentMethod;
                String accNumber = cardData.getNumber();
                request.addRange(StringUtils.padRight(accNumber, 19, ' '), 19);
                NtsUtils.log("Account No", StringUtils.padRight(accNumber, 19, ' '));
                request.addRange(cardData.getShortExpiry(), 4);
                NtsUtils.log("Expiration Date", cardData.getShortExpiry());

            } else if (paymentMethod instanceof ITrackData) {
                ITrackData trackData = (ITrackData) paymentMethod;
                if (trackData.getPan() != null) {
                    // Account number
                    NtsUtils.log("Account Number", StringUtils.padRight(trackData.getPan(), 19, ' '));
                    request.addRange(StringUtils.padRight(trackData.getPan(), 19, ' '), 19);

                    String expiryDate = NtsUtils.prepareExpDateWithoutTrack(trackData.getExpiry());
                    // Expiry date
                    NtsUtils.log("Expiry Date", StringUtils.padRight(expiryDate, 4, ' '));
                    request.addRange(StringUtils.padRight(expiryDate, 4, ' '), 4);
                } else {
                    NtsUtils.log("TrackData 2", StringUtils.padRight(trackData.getValue(), 40, ' '));
                    request.addRange(StringUtils.padRight(trackData.getValue(), 40, ' '), 40);
                }
            } else if (paymentMethod instanceof GiftCard) {
                GiftCard gift = (GiftCard) paymentMethod;
                // Account number
                NtsUtils.log("Account Number", StringUtils.padRight(gift.getPan(), 19, ' '));
                request.addRange(StringUtils.padRight(gift.getPan(), 19, ' '), 19);

                String expiryDate = NtsUtils.prepareExpDateWithoutTrack(gift.getExpiry());
                // Expiry date
                NtsUtils.log("Exp Date", StringUtils.padRight(expiryDate, 4, ' '));
                request.addRange(StringUtils.padRight(expiryDate, 4, ' '), 4);
            }

            request.addRange(ntsDataCollectRequest.getApprovalCode(), 6);
            NtsUtils.log("ApprovalCode", ntsDataCollectRequest.getApprovalCode());

            request.addRange(ntsDataCollectRequest.getAuthorizer().getValue(), 1);
            NtsUtils.log("Authorizer", ntsDataCollectRequest.getAuthorizer().getValue());

            request.addRange(StringUtils.toNumeric(ntsDataCollectRequest.getAmount(), 6), 7);
            NtsUtils.log("Amount", StringUtils.toNumeric(ntsDataCollectRequest.getAmount(), 6));

            request.addRange(ntsDataCollectRequest.getMessageCode().getValue(), 2);
            NtsUtils.log("MessageCode", ntsDataCollectRequest.getMessageCode().getValue());

            request.addRange(ntsDataCollectRequest.getAuthorizationResponseCode(), 2);
            NtsUtils.log("AuthorizationResponseCode", ntsDataCollectRequest.getAuthorizationResponseCode());

            request.addRange(ntsRequestMessageHeader.getTransactionDate(), 4);
            NtsUtils.log("OriginalTransactionDate", ntsRequestMessageHeader.getTransactionDate());

            request.addRange(ntsRequestMessageHeader.getTransactionTime(), 6);
            NtsUtils.log("OriginalTransactionTime", ntsRequestMessageHeader.getTransactionTime());

            if (batchNumber == 0 && batchProvider != null) {
                batchNumber = batchProvider.getBatchNumber();
            }
            //BatchNumber
            request.addRange(batchNumber, 2);
            NtsUtils.log("Batch Number", String.valueOf(batchNumber));

            if (!builder.getTransactionType().equals(TransactionType.BatchClose)) {
                sequenceNumber = builder.getSequenceNumber();
                if (sequenceNumber == 0 && batchProvider != null) {
                    sequenceNumber = batchProvider.getSequenceNumber();
                }
            }
            //Sequence Number
            request.addRange(StringUtils.padLeft(sequenceNumber, 3, '0'), 3);
            NtsUtils.log("Sequence Number", String.valueOf(sequenceNumber));

            if (!StringUtils.isNullOrEmpty(userData)) {
                if (userData.length() > 99 && userData.length() <= 170) {
                    request.addRange(userData.length(), 3);
                    NtsUtils.log("User Data Length", Integer.toString(userData.length()));
                } else if (userData.length() > 170 ){
                    // Extended user data flag
                    request.addRange("E", 1);
                    NtsUtils.log("Extended user data flag", "E");

                    // User data length
                    request.addRange(userData.length(), 4);
                    NtsUtils.log("User Data Length", Integer.toString(userData.length()));
                } else {
                    request.addRange(userData.length(), 2);
                    NtsUtils.log("User Data Length", Integer.toString(userData.length()));
                }

                request.addRange(userData, userData.length());
                NtsUtils.log("User Data ", userData);
            }

        }
        return request;
    }
}
