package com.portal.procucev.rfq;

import com.portal.procucev.rfq.service.EmailReaderService;
import jakarta.mail.FetchProfile;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.search.SearchTerm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cover for the IMAP "BYE ... Read timed out" failures seen on 17-18 Aug 2026.
 *
 * <p>Filing a processed message used to walk every message in the inbox calling
 * {@code getHeader("Message-ID")}. That header is not prefetched on an IMAP folder, so each call
 * was its own network round trip and a 50-message inbox regularly exceeded the socket read
 * timeout. These tests pin the two properties that matter: a hit costs one server-side search, and
 * the fallback path costs one bulk FETCH rather than one round trip per message.
 */
public class ImapMessageLookupTest {

    private static final String TARGET_ID = "CADmWLxiajUSUBVrLTSse_WT2jGgG=qSx1F9Hcz8yQ6mh4q4fzA@mail.gmail.com";

    private EmailReaderService service;

    @BeforeEach
    void setUp() {
        service = new EmailReaderService();
        ReflectionTestUtils.setField(service, "inboxFolder", "INBOX");
    }

    private Message findById(Folder folder, String messageId) {
        return ReflectionTestUtils.invokeMethod(service, "findMessageById", folder, messageId);
    }

    @Test
    @DisplayName("A server-side search hit is used directly, with no mailbox walk and no per-message header read")
    void searchHitAvoidsMailboxWalk() throws MessagingException {
        Folder folder = mock(Folder.class);
        Message hit = mock(Message.class);
        when(folder.search(any(SearchTerm.class))).thenReturn(new Message[]{hit});

        assertSame(hit, findById(folder, "<" + TARGET_ID + ">"));

        verify(folder, never()).getMessages();
        verify(folder, never()).fetch(any(Message[].class), any(FetchProfile.class));
        verify(hit, never()).getHeader(anyString());
    }

    @Test
    @DisplayName("When the search finds nothing the fallback scan prefetches headers in a single bulk FETCH")
    void fallbackScanUsesOneBulkFetch() throws MessagingException {
        Folder folder = mock(Folder.class);
        when(folder.search(any(SearchTerm.class))).thenReturn(new Message[0]);

        Message older = mock(Message.class);
        Message newest = mock(Message.class);
        when(newest.getHeader("Message-ID")).thenReturn(new String[]{"<" + TARGET_ID + ">"});
        when(folder.getMessages()).thenReturn(new Message[]{older, newest});

        assertSame(newest, findById(folder, TARGET_ID));

        verify(folder, times(1)).fetch(any(Message[].class), any(FetchProfile.class));
        // Newest first: the message just processed is the latest arrival, so older mail is not read.
        verify(older, never()).getHeader(anyString());
    }

    @Test
    @DisplayName("A server that rejects HEADER SEARCH still resolves through the fallback scan")
    void searchFailureFallsBackToScan() throws MessagingException {
        Folder folder = mock(Folder.class);
        when(folder.search(any(SearchTerm.class))).thenThrow(new MessagingException("SEARCH not supported"));

        Message hit = mock(Message.class);
        when(hit.getHeader("Message-ID")).thenReturn(new String[]{TARGET_ID});
        when(folder.getMessages()).thenReturn(new Message[]{hit});

        assertSame(hit, findById(folder, TARGET_ID));
        verify(folder, times(1)).fetch(any(Message[].class), any(FetchProfile.class));
    }

    @Test
    @DisplayName("Angle brackets on either side are treated as the same Message-ID")
    void messageIdBracketsAreIgnored() throws MessagingException {
        Folder folder = mock(Folder.class);
        when(folder.search(any(SearchTerm.class))).thenReturn(new Message[0]);

        Message hit = mock(Message.class);
        when(hit.getHeader("Message-ID")).thenReturn(new String[]{"  <" + TARGET_ID + ">  "});
        when(folder.getMessages()).thenReturn(new Message[]{hit});

        assertSame(hit, findById(folder, TARGET_ID));
    }

    @Test
    @DisplayName("A message absent from the inbox yields null instead of an endless search")
    void absentMessageYieldsNull() throws MessagingException {
        Folder folder = mock(Folder.class);
        when(folder.search(any(SearchTerm.class))).thenReturn(new Message[0]);

        Message other = mock(Message.class);
        when(other.getHeader("Message-ID")).thenReturn(new String[]{"<someone.else@mail.test>"});
        Message headerless = mock(Message.class);
        when(headerless.getHeader("Message-ID")).thenReturn(null);
        when(folder.getMessages()).thenReturn(new Message[]{other, headerless});

        assertNull(findById(folder, TARGET_ID));
    }

    @Test
    @DisplayName("An empty inbox and an unusable Message-ID are both handled without touching the mailbox")
    void emptyInboxAndBlankIdAreHandled() throws MessagingException {
        Folder emptyFolder = mock(Folder.class);
        when(emptyFolder.search(any(SearchTerm.class))).thenReturn(new Message[0]);
        when(emptyFolder.getMessages()).thenReturn(new Message[0]);
        assertNull(findById(emptyFolder, TARGET_ID));
        verify(emptyFolder, never()).fetch(any(Message[].class), any(FetchProfile.class));

        Folder untouched = mock(Folder.class);
        assertNull(findById(untouched, "<>"));
        assertNull(findById(untouched, "   "));
        verify(untouched, never()).search(any(SearchTerm.class));
        verify(untouched, never()).getMessages();
    }
}
